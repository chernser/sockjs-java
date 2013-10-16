/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.apache.commons.io.IOUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

public class HttpHelpers {

    private final static HttpResponse IFRAME_HTML;

    static {
        try {
            URL iframeResource =  Thread.currentThread().getContextClassLoader().getResource("sockjs_iframe.html");
            byte[] iframeContent = IOUtils.toByteArray(new FileInputStream(new File(iframeResource.getPath())));
            ChannelBuffer iframeContentBuffer = ChannelBuffers.wrappedBuffer(iframeContent);

            IFRAME_HTML = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            IFRAME_HTML.setContent(iframeContentBuffer);
            IFRAME_HTML.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=utf8");
            IFRAME_HTML.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
            IFRAME_HTML.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");

        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public static void sendText(ChannelHandlerContext ctx, HttpResponseStatus status, String body) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        response.setContent(ChannelBuffers.copiedBuffer(body, CharsetUtil.UTF_8));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        response.setHeader(HttpHeaders.Names.CONNECTION, "close");
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status, String body) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        response.setContent(ChannelBuffers.copiedBuffer(body, CharsetUtil.UTF_8));
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendIFrameHtml(ChannelHandlerContext ctx) {
        ctx.getChannel().write(IFRAME_HTML).addListener(ChannelFutureListener.CLOSE);
    }
}
