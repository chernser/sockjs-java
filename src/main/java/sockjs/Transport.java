package sockjs;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.transports.Protocol;

public interface Transport {

    void handle(ChannelHandlerContext ctx, HttpRequest httpRequest);

    void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame);

    void sendHeartbeat(Channel channel);

    void sendMessage(Channel channel, Message message);

    void close(Channel channel, Protocol.CloseReason reason);
}
