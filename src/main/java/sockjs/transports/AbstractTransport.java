/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.SockJs;
import sockjs.Transport;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsHandlerContext;

public abstract class AbstractTransport implements Transport {

    private final SockJs sockJs;

    protected AbstractTransport(SockJs sockJs) {
         this.sockJs = sockJs;
    }

    public SockJs getSockJs() {
        return sockJs;
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    protected SockJsHandlerContext getSockJsHandlerContext(Channel channel) {
        return getSockJsHandlerContext(channel.getPipeline().getContext("handler"));
    }

    protected SockJsHandlerContext getSockJsHandlerContext(ChannelHandlerContext ctx) {
        Object attachment = ctx.getAttachment();
        if (attachment != null && attachment instanceof SockJsHandlerContext) {
            return (SockJsHandlerContext)attachment;
        }
        return null;
    }
}
