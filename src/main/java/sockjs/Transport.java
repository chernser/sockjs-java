package sockjs;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.transports.Protocol;

public interface Transport {

    void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest);

    void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame);

    void sendHeartbeat(Connection connection);

    void sendMessage(Connection connection, String encodedMessage);

    void handleCloseRequest(Connection connection, Protocol.CloseReason reason);
}
