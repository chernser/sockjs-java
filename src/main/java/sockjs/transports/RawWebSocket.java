package sockjs.transports;

import org.jboss.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;

public class RawWebSocket extends WebSocket {

    private static final Logger log = LoggerFactory.getLogger(RawWebSocket.class);

    public RawWebSocket(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void sendMessage(Connection connection, String[] messagesToSend) {
        for (String message : messagesToSend) {
            connection.getChannel().write(new TextWebSocketFrame(message));
        }
    }

}
