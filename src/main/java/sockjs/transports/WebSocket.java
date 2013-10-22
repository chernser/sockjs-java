/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsHandlerContext;

public class WebSocket extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    public static final String ERR_INCORRECT_UPGRADE = "\"Connection\" must be \"Upgrade\".";

    public static final String ERR_INVALID_REQUEST = "Can \"Upgrade\" only to \"WebSocket\".";

    private final ChannelFutureListener INIT_CONNECTION = new InitConnection();

    private final ChannelFutureListener SEND_OPEN_FRAME = new SendOpenFrame();

    public WebSocket(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        log.info("exception: ", e.getCause());
        try {
            if (ctx.getChannel().isWritable()) {
//                ctx.getChannel().write(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
            } else {
                ctx.getChannel().close();
            }
        } catch (Exception ex) {
            log.error(">> ", ex);
        }
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        if (httpRequest.getMethod() != HttpMethod.GET) {
            HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED);
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
            response.setHeader(HttpHeaders.Names.ALLOW, "GET");
            response.setHeader(HttpHeaders.Names.CONNECTION, "close");
            ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
            return;
        }

        if (isWebSocketUpgrade(httpRequest)) {
            if (!isValidConnectionHeader(httpRequest)) {
                HttpHelpers.sendError(ctx, HttpResponseStatus.BAD_REQUEST, ERR_INCORRECT_UPGRADE);
            } else {
                // upgrade & handshake
                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                        getWebSocketLocation(httpRequest, "/"), null, false);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);
                if (handshaker == null) {
                    wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
                } else {
                    if (WebSocketVersion.V08.equals(handshaker.getVersion())) {
                        handshaker.handshake(ctx.getChannel(), httpRequest).addListener(INIT_CONNECTION);
                        ctx.getPipeline().replace(this, "handler", new RawWebSocket(getSockJs()));
                    } else {
                        handshaker.handshake(ctx.getChannel(), httpRequest).addListener(SEND_OPEN_FRAME);
                    }
                }
            }
        } else {
            HttpHelpers.sendError(ctx, HttpResponseStatus.BAD_REQUEST, ERR_INVALID_REQUEST);
        }
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext == null) {
            log.error("no sockjs handler context for channel");
            return;
        }
        if (sockJsHandlerContext.getConnection() == null) {
            log.error("no sockjs connection for channel");
            return;
        }

        if (webSocketFrame instanceof TextWebSocketFrame) {
            log.info("text frame received: " + webSocketFrame);
            try {
                String[] messages = Protocol
                        .decodeMessage(((TextWebSocketFrame) webSocketFrame).getText());
                if (messages != null) {
                    for (String message : messages) {
                        sockJsHandlerContext.getConnection().sendToListeners(message);
                    }
                } else {
                    handleCloseRequest(sockJsHandlerContext
                            .getConnection(), Protocol.CloseReason.NORMAL);
                }
            } catch (JsonParseException ex) {
                handleCloseRequest(sockJsHandlerContext
                        .getConnection(), Protocol.CloseReason.NORMAL);
            }
        } else if (webSocketFrame instanceof PingWebSocketFrame) {
            ctx.getChannel().write(new PongWebSocketFrame(webSocketFrame.getBinaryData()));
        } else if (webSocketFrame instanceof CloseWebSocketFrame) {
            sockJsHandlerContext.getConnection().setCloseReason(Protocol.CloseReason.NORMAL);
        } else {
            log.error("Unknown frame received: " + webSocketFrame);
        }
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        connection.getChannel().write(Protocol.WEB_SOCKET_HEARTBEAT_FRAME);
    }

    @Override
    public void sendMessage(Connection connection, String message) {
        log.info("handling send message: " + message);
        connection.getChannel().write( new TextWebSocketFrame(message));
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().write(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
    }

    private boolean isWebSocketUpgrade(HttpRequest httpRequest) {
        String upgradeHeader = httpRequest.getHeader(HttpHeaders.Names.UPGRADE);
        return upgradeHeader != null && "WebSocket".compareToIgnoreCase(upgradeHeader) == 0;
    }

    private boolean isValidConnectionHeader(HttpRequest httpRequest) {
        String connectionHeader = httpRequest.getHeader(HttpHeaders.Names.CONNECTION);
        return connectionHeader != null && connectionHeader.contains("Upgrade");
    }

    private static String getWebSocketLocation(HttpRequest req, String path) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + path;
    }

    private class InitConnection implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(future.getChannel());
            if (sockJsHandlerContext != null) {
                Connection connection = getSockJs().createConnection(sockJsHandlerContext);
                connection.setChannel(future.getChannel());
                connection.startHeartbeat();
                sockJsHandlerContext.setConnection(connection);
                WebSocket.this.getSockJs().notifyListenersAboutNewConnection(connection);
            } else {
                log.error("no sockjs handler context for channel");
            }
        }
    }

    private class SendOpenFrame extends InitConnection {

        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            super.operationComplete(future);
            if (future.getChannel().isWritable()) {
                future.getChannel().write(Protocol.WEB_SOCKET_OPEN_FRAME);
            }
        }
    }
}
