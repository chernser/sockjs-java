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
import sockjs.netty.HttpHelpers;

public class WebSocket extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(WebSocket.class);

    public static final String ERR_INCORRECT_UPGRADE = "\"Connection\" must be \"Upgrade\".";

    public static final String ERR_INVALID_REQUEST = "Can \"Upgrade\" only to \"WebSocket\".";

    private final ChannelFutureListener ON_HANDSHAKE_FINISHED = new HandshakeFinishedListener();

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
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
    }

    @Override
    public void sendHeartbeat(Channel channel) {
        channel.write(Protocol.WEB_SOCKET_HEARTBEAT_FRAME);
    }

    @Override
    public void sendMessage(Channel channel, Message message) {
        channel.write(Protocol.encodeMessageToWebSocketFrame(message));
    }

    private boolean isWebSocketUpgrade(HttpRequest httpRequest) {
        return "WebSocket".compareToIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.UPGRADE)) == 0;
    }

    private boolean isValidConnectionHeader(HttpRequest httpRequest) {
        return "Upgrade".compareToIgnoreCase(httpRequest.getHeader(HttpHeaders.Names.CONNECTION)) == 0;
    }

    private static String getWebSocketLocation(HttpRequest req, String path) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + path;
    }

    private class HandshakeFinishedListener implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            future.getChannel().write(Protocol.WEB_SOCKET_OPEN_FRAME);
            Connection connection = new Connection(future.getChannel(), WebSocket.this);
            connection.startHeartbeat();
        }
    }
}
