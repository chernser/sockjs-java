package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;
import sockjs.netty.SockJsHandlerContext;

public class RawWebSocket extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocket.class);

    public RawWebSocket(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        connection.getChannel().write(Protocol.WEB_SOCKET_HEARTBEAT_FRAME + "\uffff");
    }

    @Override
    public void sendMessage(Connection connection, String encodedMessage) {
        log.info("sending via raw web socket: " +encodedMessage);
        connection.getChannel().write(new TextWebSocketFrame(encodedMessage + "\uffff"));
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().write(new CloseWebSocketFrame()).addListener(ChannelFutureListener.CLOSE);
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
}
