package sockjs.netty;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.SockJs;


public class WebSocketFrameHandler extends SimpleChannelHandler {

    private final SockJs sockJs;

    public WebSocketFrameHandler(SockJs sockJs) {
        this.sockJs = sockJs;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof WebSocketFrame) {
            handleFrame(ctx, (WebSocketFrame)msg);
        }
    }

    private void handleFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

        if (frame instanceof CloseWebSocketFrame) {

        }

    }
}
