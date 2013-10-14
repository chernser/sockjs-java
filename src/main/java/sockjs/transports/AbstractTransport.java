/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.Transport;
import sockjs.netty.HttpHelpers;

public class AbstractTransport implements Transport {

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void sendHeartbeat(Channel channel) {
    }
}
