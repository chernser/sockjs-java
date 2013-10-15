package sockjs.netty;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.SockJs;
import sockjs.Transport;

/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
public class WebSocketHandler extends SimpleChannelHandler {

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof WebSocketFrame) {
            handleFrame(ctx, (WebSocketFrame)msg);
        }
    }

    private void handleFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

    }

}
