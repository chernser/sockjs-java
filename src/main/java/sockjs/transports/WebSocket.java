/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.Transport;

public class WebSocket extends AbstractTransport {

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {


        super.handle(ctx, httpRequest);    //To change body of overridden methods use File |
        // Settings | File Templates.
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        super.handle(ctx, webSocketFrame);    //To change body of overridden methods use File |
        // Settings | File Templates.
    }
}
