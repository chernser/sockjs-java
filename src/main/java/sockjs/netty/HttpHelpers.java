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
import sun.security.provider.MD5;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class HttpHelpers {

    private final static HttpResponse IFRAME_HTML;

    private final static HttpResponse IFRAME_NOT_CHANGED;

    public final static String IFRAME_ETAG;

    public final static int YEAR_IN_SEC = 31536000;

    static {
        try {
            URL iframeResource =  Thread.currentThread().getContextClassLoader().getResource("sockjs_iframe.html");
            File iframeHtmlFile = new File(iframeResource.getPath());
            byte[] iframeContent = IOUtils.toByteArray(new FileInputStream(iframeHtmlFile));
            String lastModified = new Date(iframeHtmlFile.lastModified()).toString();

            IFRAME_ETAG = initIFrameETag(iframeContent);
            IFRAME_HTML = initIFrameResponse(iframeContent, lastModified);
            IFRAME_NOT_CHANGED = initIFrameNotChangedResponse(lastModified);

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
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, body.length());
        response.setContent(ChannelBuffers.copiedBuffer(body, CharsetUtil.UTF_8));
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendOptions(ChannelHandlerContext ctx, HttpRequest req, String allowedMethods) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_MAX_AGE, HttpHelpers.YEAR_IN_SEC);

        String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        if (origin == null || origin.equals("null") || origin.isEmpty()) {
            origin = "*";
        }
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=31536000");
        String expires = new Date(System.currentTimeMillis() + (HttpHelpers.YEAR_IN_SEC * 1000L)).toString();
        response.setHeader(HttpHeaders.Names.EXPIRES, expires);

        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void sendIFrameHtml(ChannelHandlerContext ctx, String etag) {
        if (IFRAME_ETAG.equals(etag)) {
            ctx.getChannel().write(IFRAME_NOT_CHANGED).addListener(ChannelFutureListener.CLOSE);
        } else {
            ctx.getChannel().write(IFRAME_HTML).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static String initIFrameETag(byte[] iframeContent) throws NoSuchAlgorithmException {

        // Create etag for iframe
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(iframeContent);

        byte[] hash = md5.digest();
        StringBuilder etagHashStringBuilder = new StringBuilder(32);
        for (byte hashByte : hash) {
            etagHashStringBuilder.append(Integer.toHexString(((int) hashByte) & 0xff));
        }

        return etagHashStringBuilder.toString();
    }

    private static HttpResponse initIFrameResponse(byte[] iframeContent, String lastModified)
            throws IOException {

        ChannelBuffer iframeContentBuffer = ChannelBuffers.wrappedBuffer(iframeContent);

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setContent(iframeContentBuffer);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, iframeContent.length);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=31536000");
        //response.setHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
        response.setHeader(HttpHeaders.Names.ETAG, IFRAME_ETAG);
        String expires = new Date(System.currentTimeMillis() + (YEAR_IN_SEC * 1000L)).toString();
        response.setHeader(HttpHeaders.Names.EXPIRES, expires);
        return response;
    }

    private static HttpResponse initIFrameNotChangedResponse(String lastModified) {

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, true);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=31536000");
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, lastModified);
        response.setHeader(HttpHeaders.Names.ETAG, IFRAME_ETAG);

        return response;
    }
}
