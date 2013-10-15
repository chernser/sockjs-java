/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Message;
import sockjs.SockJs;
import sockjs.Transport;

public class XHttpRequest extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(XHttpRequest.class);

    public XHttpRequest(SockJs sockJs) {
        super(sockJs);
    }


    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        log.info("Handling new request: ");
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, httpRequest.getHeader(HttpHeaders.Names.ORIGIN));
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, "OPTIONS, POST");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        response.setContent(ChannelBuffers.copiedBuffer("o", CharsetUtil.UTF_8));

        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void sendHeartbeat(Channel channel) {

    }

    @Override
    public void sendMessage(Channel channel, Message message) {

    }
}
