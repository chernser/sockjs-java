package sockjs.transports;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;

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
}
