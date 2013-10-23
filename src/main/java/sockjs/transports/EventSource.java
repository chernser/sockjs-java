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
import sockjs.SockJs;
import sockjs.netty.HttpHelpers;
import sockjs.netty.SockJsHandlerContext;

public class EventSource extends XHttpRequest {

    private static final Logger log = LoggerFactory.getLogger(EventSource.class);

    private static final HttpChunk PRELUDE_CHUNK;

    private static final ChannelFutureListener SEND_LAST_CHUNK;

    static {
        PRELUDE_CHUNK = new DefaultHttpChunk(ChannelBuffers.copiedBuffer("\r\n", CharsetUtil.UTF_8));
        SEND_LAST_CHUNK = new SendLastChunk();

    }

    public EventSource(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        if (httpRequest.getMethod() == HttpMethod.GET) {
            handleStreaming(ctx, httpRequest);
        } else if (httpRequest.getMethod() == HttpMethod.OPTIONS) {
            HttpHelpers.sendOptions(ctx, httpRequest, "OPTIONS, GET");
        }
    }

    @Override
    public void sendMessage(Connection connection, String[] messagesToSend) {
        String message = Protocol.encodeMessageToString(messagesToSend);
        ChannelBuffer content = ChannelBuffers
                .copiedBuffer("data: " + message + "\r\n\r\n", CharsetUtil.UTF_8);
        ChannelFuture writeFuture = connection.getChannel().write(new DefaultHttpChunk(content));
        connection.incSentBytes(content.readableBytes());

        if (connection.getSentBytes() > getSockJs().getMaxStreamSize()) {
            connection.resetSentBytes();
            writeFuture.addListener(SEND_LAST_CHUNK);
        }
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().close();
    }

    private void handleStreaming(ChannelHandlerContext ctx, HttpRequest httpRequest) {

        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            HttpResponse response = createStreamingResponse(httpRequest);
            HttpHelpers.addJESSIONID(response, sockJsHandlerContext.getJSESSIONID());
            ctx.getChannel().write(response).addListener(new SendPrelude(new SendOpen()));
        } else {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    protected static HttpResponse createStreamingResponse(HttpRequest httpRequest) {
        HttpResponse response = XHttpRequest.createStreamingResponse(httpRequest);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/event-stream;charset=UTF-8");

        return response;
    }

    private static class SendPrelude implements ChannelFutureListener {

        private ChannelFutureListener nextListener;

        private SendPrelude(ChannelFutureListener nextListener) {
            this.nextListener = nextListener;
        }

        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {

            future.getChannel().write(PRELUDE_CHUNK).addListener(nextListener);
        }
    }

    private class SendOpen implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            log.info("Sending open");
            HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers
                    .copiedBuffer("data: o\r\n\r\n", CharsetUtil.UTF_8));

            SockJsHandlerContext sockJsHandlerContext = EventSource.this
                    .getSockJsHandlerContext(future.getChannel());
            if (sockJsHandlerContext != null) {
                Connection connection = sockJsHandlerContext.getConnection();
                if (connection == null) {
                    connection = EventSource.this.getSockJs().createConnection(sockJsHandlerContext);
                    connection.setJSESSIONID(sockJsHandlerContext.getJSESSIONID());
                }

                connection.setChannel(future.getChannel());

                final Connection newConnection = connection;
                future.getChannel().write(chunk).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future)
                            throws Exception {
                        EventSource.this.getSockJs()
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
