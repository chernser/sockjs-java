/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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
import sockjs.netty.WebSocketHandshakerFactory;

import java.util.UUID;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.WEBSOCKET_PROTOCOL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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
        try {
            log.error("exception:", e.getCause());
            if (!ctx.getChannel().isWritable()) {
                ctx.getChannel().close();
            }
        } catch (Exception ex) {

        }
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        if (httpRequest.getMethod() != HttpMethod.GET) {
            HttpHelpers.sendMethodNotAllowed(ctx, "GET");
            return;
        }

        if (isWebSocketUpgrade(httpRequest)) {
            if (!isValidConnectionHeader(httpRequest)) {
                HttpHelpers.sendError(ctx, HttpResponseStatus.BAD_REQUEST, ERR_INCORRECT_UPGRADE);
            } else {
                // upgrade & handshake
                WebSocketServerHandshakerFactory wsFactory = new WebSocketHandshakerFactory(
                        getWebSocketLocation(httpRequest, "/"), null, false);
                WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(httpRequest);
                if (handshaker == null) {
                    wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
                } else {
                    if (getSockJsHandlerContext(ctx).getSessionId() == null) {
                        getSockJsHandlerContext(ctx).setSessionId(UUID.randomUUID().toString());
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
    public void sendMessage(Connection connection, String[] messagesToSend) {
        String message = Protocol.encodeMessageToString(messagesToSend);
        log.info("handling send message: " + message);
        connection.getChannel().write( new TextWebSocketFrame(message));
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        final CloseWebSocketFrame closeWebSocketFrame = new CloseWebSocketFrame(1000, reason.frame);
        connection.getChannel().write(closeWebSocketFrame).addListener(ChannelFutureListener.CLOSE);
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
                SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(future.getChannel());
                WebSocket.this.getSockJs().notifyListenersAboutNewConnection(sockJsHandlerContext.getConnection());
            }
        }
    }
}
