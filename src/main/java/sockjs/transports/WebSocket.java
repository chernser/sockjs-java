/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import com.sun.xml.internal.ws.util.StringUtils;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.jboss.netty.util.internal.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.Message;
import sockjs.SockJs;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsHandlerContext;

public class WebSocket extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    public static final String ERR_INCORRECT_UPGRADE = "\"Connection\" must be \"Upgrade\".";

    public static final String ERR_INVALID_REQUEST = "Can \"Upgrade\" only to \"WebSocket\".";

    private final ChannelFutureListener ON_HANDSHAKE_FINISHED = new HandshakeFinishedListener();

    public WebSocket(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
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
                    handshaker.handshake(ctx.getChannel(), httpRequest).addListener(ON_HANDSHAKE_FINISHED);
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
            String[] messages = Protocol.decodeMessage(((TextWebSocketFrame) webSocketFrame).getText());
            if (messages != null) {
                for (String message : messages) {
                    sockJsHandlerContext.getConnection().sendToListeners(message);
                }
            }
        }
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        connection.getChannel().write(Protocol.WEB_SOCKET_HEARTBEAT_FRAME);
    }

    @Override
    public void sendMessage(Connection connection, String message) {
        connection.getChannel().write(message);
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().write(reason.webSocketFrame).addListener(ChannelFutureListener.CLOSE);
    }

    private boolean isWebSocketUpgrade(HttpRequest httpRequest) {
        String upgradeHeader = httpRequest.getHeader(HttpHeaders.Names.UPGRADE);
        return upgradeHeader != null && "WebSocket".compareToIgnoreCase(upgradeHeader) == 0;
    }

    private boolean isValidConnectionHeader(HttpRequest httpRequest) {
        String connectionHeader = httpRequest.getHeader(HttpHeaders.Names.CONNECTION);
        return connectionHeader != null && "Upgrade".compareToIgnoreCase(connectionHeader) == 0;
    }

    private static String getWebSocketLocation(HttpRequest req, String path) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + path;
    }

    private class HandshakeFinishedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            future.getChannel().write(Protocol.WEB_SOCKET_OPEN_FRAME);
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
}
