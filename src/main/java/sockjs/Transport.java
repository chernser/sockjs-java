package sockjs;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface Transport {

    void handle(ChannelHandlerContext ctx, HttpRequest httpRequest);

    void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame);

}
