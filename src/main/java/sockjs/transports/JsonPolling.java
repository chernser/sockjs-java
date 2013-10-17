/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.Message;
import sockjs.SockJs;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsHandlerContext;

import java.util.List;

public class JsonPolling extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(JsonPolling.class);

    private static final ChannelFutureListener SEND_LAST_CHUNK;

    static {
        SEND_LAST_CHUNK = new SendLastChunk();

    }

    public JsonPolling(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        if (httpRequest.getMethod() == HttpMethod.GET) {
            handleJsonPolling(ctx, httpRequest);
        } else if (httpRequest.getMethod() == HttpMethod.POST) {
            handleJsonPollingSend(ctx, httpRequest);
        }
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendMessage(Connection connection, Message message) {
        ChannelBuffer content = ChannelBuffers.copiedBuffer(connection.getJsonpCallback() + "(\"" +
                Protocol.encodeMessageToString(message) + "\");\r\n", CharsetUtil.UTF_8);

        ChannelFuture writeFuture = connection.getChannel().write(new DefaultHttpChunk(content));
        connection.incSentBytes(content.readableBytes());

        if (connection.getSentBytes() > getSockJs().getMaxStreamSize()) {
            connection.resetSentBytes();
            writeFuture.addListener(SEND_LAST_CHUNK);
        }
    }

    @Override
    public void close(Connection connection, Protocol.CloseReason reason) {
        ChannelBuffer content = ChannelBuffers.copiedBuffer(Protocol
                .encodeJsonpClose(reason, connection.getJsonpCallback()), CharsetUtil.UTF_8);
        connection.getChannel().write(new DefaultHttpChunk(content)).addListener(SEND_LAST_CHUNK);
    }

    private void handleJsonPollingSend(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx.getChannel());
        if (sockJsHandlerContext != null) {
            String message = httpRequest.getContent().toString(CharsetUtil.UTF_8);
            log.info("Message received: " + message);
            Connection connection = sockJsHandlerContext.getConnection();
            if (connection != null) {
                Message[] messages = Protocol.decodeMessage(message);
                for (Message decodedMessage : messages) {
                    connection.sendToListeners(decodedMessage);
                }
            } else {
                HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
        }

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        String origin = httpRequest.getHeader(HttpHeaders.Names.ORIGIN);
        if (origin != null) {
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        } else {
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-store, no-cache, must-revalidate," +
                "" + " max-age=0");
        response.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer("ok", CharsetUtil.UTF_8));

        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleJsonPolling(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        List<String> callbacks = new QueryStringDecoder(httpRequest.getUri()).getParameters().get("c");
        if (callbacks == null || callbacks.isEmpty()) {
            HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                    "\"callback\" parameter required");
            return;
        }

        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            HttpResponse response = createResponse(ctx, httpRequest);
            sockJsHandlerContext.setJsonpCallback(callbacks.get(0));
            ctx.getChannel().write(response).addListener(new SendOpen());
        } else {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }

    }

    private static HttpResponse createResponse(ChannelHandlerContext ctx,
                                               HttpRequest httpRequest) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        String origin = httpRequest.getHeader(HttpHeaders.Names.ORIGIN);
        if (origin != null) {
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        } else {
            response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        }

        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-store, no-cache, must-revalidate," +
                "" + " max-age=0");
        response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        response.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript;charset=UTF-8");

        return response;
    }

    private class SendOpen implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            log.info("Sending open");
            SockJsHandlerContext sockJsHandlerContext = JsonPolling.this
                    .getSockJsHandlerContext(future.getChannel());
            if (sockJsHandlerContext != null) {
                Connection connection = sockJsHandlerContext.getConnection();
                if (connection == null) {
                    connection = JsonPolling.this.getSockJs().createConnection(sockJsHandlerContext
                            .getBaseUrl(), sockJsHandlerContext.getSessionId());
                }

                connection.setChannel(future.getChannel());
                connection.setTransport(JsonPolling.this);
                connection.setJsonpCallback(sockJsHandlerContext.getJsonpCallback());


                HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers
                        .copiedBuffer(connection.getJsonpCallback() + "(\"o\");\r\n", CharsetUtil.UTF_8));

                final Connection newConnection = connection;
                future.getChannel().write(chunk).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future)
                            throws Exception {
                        JsonPolling.this.getSockJs()
                                .notifyListenersAboutNewConnection(newConnection);
                    }
                });

            }
        }
    }

    private static class SendLastChunk implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            future.getChannel().write(HttpChunk.LAST_CHUNK).addListener(CLOSE);
        }
    }
}
