/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import sockjs.Transport;

public class AbstractTransport implements Transport {

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        sendError(ctx, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

}
