/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.SockJs;
import sockjs.Transport;

public class HttpHandler extends SimpleChannelHandler {

    private static final Logger log = LoggerFactory.getLogger(HttpHandler.class);

    private final SockJs sockJs;

    private boolean credentialAllowed = true;

    private int maxAge = 31536000;


    public HttpHandler(SockJs sockJs) {
        this.sockJs = sockJs;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            handleRequest(ctx, (HttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleFrame(ctx, (WebSocketFrame)msg);
        }
    }

    private void handleRequest(ChannelHandlerContext ctx, HttpRequest req) {
        if (req.getMethod() == HttpMethod.OPTIONS) {
            log.info("sending options for: " + req.getUri());
            sendOptions(ctx, new HttpMethod[] {HttpMethod.OPTIONS, HttpMethod.POST});
            return;
        }

        if (!sockJs.hasListenerForRoute(req.getUri())) {
            HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }

        if (sockJs.isRootOfBaseUrl(req.getUri())) {
            if (req.getMethod() == HttpMethod.GET) {
                sendGreeting(ctx);
                return;
            } else if (req.getMethod() != HttpMethod.GET) {
                HttpHelpers.sendError(ctx, HttpResponseStatus.METHOD_NOT_ALLOWED);
                return;
            }
        }

        if (req.getMethod().equals(HttpMethod.OPTIONS)) {
            log.info("sending options for: " + req.getUri());
            sendOptions(ctx, new HttpMethod[] {HttpMethod.OPTIONS, HttpMethod.POST});
            return;
        }

        if (req.getUri().endsWith("/info") && req.getMethod() == HttpMethod.GET) {
            sendInfo(ctx);
            return;
        }

        Transport transport = sockJs.getTransport(getTransport(req.getUri()));
        if (transport == null) {
            HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_FOUND);
            return;
        }


        SockJsHandlerContext sockJsHandlerContext = new SockJsHandlerContext();
        sockJsHandlerContext.setBaseUrl(sockJs.getBaseUrl(req.getUri()));
        ctx.setAttachment(sockJsHandlerContext);
        transport.handle(ctx, req);
    }

    private void handleFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
        Transport transport = sockJs.getTransport(SockJs.WEBSOCKET_TRANSPORT);
        if (transport == null) {
            ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
            return;
        }

        transport.handle(ctx, frame);
    }

    private void sendGreeting(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf8");
        response.setContent(ChannelBuffers.copiedBuffer("Welcome to SockJS!\\n", CharsetUtil.UTF_8));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendOptions(ChannelHandlerContext ctx, HttpMethod[] allowedMethods) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, credentialAllowed);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_METHODS, allowedMethods.toString());
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_MAX_AGE, maxAge);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "public, max-age=" + maxAge);
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void sendInfo(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, credentialAllowed);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf8");
        response.setContent(ChannelBuffers.copiedBuffer(sockJs.getInfoAsString(), CharsetUtil.UTF_8));
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private String getTransport(String uri) {
        int lastSlashIndex = uri.lastIndexOf("/");
        if (lastSlashIndex > -1) {
            return uri.substring(lastSlashIndex + 1);
        }
        return null;
    }
}
