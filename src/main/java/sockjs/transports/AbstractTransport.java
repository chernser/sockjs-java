/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;
import sockjs.Transport;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsCloseEvent;
import sockjs.netty.SockJsHandlerContext;
import sockjs.netty.SockJsSendEvent;

public abstract class AbstractTransport extends SimpleChannelHandler implements Transport {

    private static final Logger log = LoggerFactory.getLogger(AbstractTransport.class);

    private final SockJs sockJs;

    protected AbstractTransport(SockJs sockJs) {
         this.sockJs = sockJs;
    }

    public SockJs getSockJs() {
        return sockJs;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleHttpRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof SockJsCloseEvent) {
            SockJsCloseEvent se = (SockJsCloseEvent)msg;
            handleCloseRequest(se.getConnection(), se.getReason());
        } else if (msg instanceof SockJsSendEvent) {
            SockJsSendEvent se = (SockJsSendEvent)msg;
            String[] messagesToSend = se.getConnection().pollAllMessages();
            if (messagesToSend.length > 0) {
                sendMessage(se.getConnection(), messagesToSend);
            }
        } else if (msg instanceof WebSocketFrame) {
            handle(ctx, (WebSocketFrame)msg);
        }
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        log.error("Exception in transport: ", e.getCause());
        ctx.getChannel().close();
    }

    public static void sendUpstream(Channel channel, Object message) {
        UpstreamMessageEvent event = new UpstreamMessageEvent(channel, message, channel.getRemoteAddress());
        channel.getPipeline().sendUpstream(event);
    }

    protected SockJsHandlerContext getSockJsHandlerContext(Channel channel) {
        return (SockJsHandlerContext) channel.getAttachment();
    }

    protected SockJsHandlerContext getSockJsHandlerContext(ChannelHandlerContext ctx) {
        return getSockJsHandlerContext(ctx.getChannel());
    }
}
