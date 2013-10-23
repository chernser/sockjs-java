/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.Message;
import sockjs.SockJs;
import sockjs.netty.*;

public class XHttpRequest extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(XHttpRequest.class);

    private static final HttpChunk HEARTBEAT_CHUNK;

    private static final HttpChunk PRELUDE_CHUNK;

    private final ChannelFutureListener SEND_OPEN;

    private static final ChannelFutureListener SEND_LAST_CHUNK;

    private static final ChannelFutureListener SEND_ALREADY_CONNECTED;

    static {
        ChannelBuffer heartbeatContent = ChannelBuffers
                .copiedBuffer(Protocol.HEARTBEAT_FRAME + "\n", CharsetUtil.UTF_8);
        HEARTBEAT_CHUNK = new DefaultHttpChunk(heartbeatContent);

        ChannelBuffer preludeBuffer = ChannelBuffers.buffer(Protocol.PRELUDE_SIZE + 1);
        char preludeChar = Protocol.HEARTBEAT_FRAME.charAt(0);
        for (int i = 0; i < Protocol.PRELUDE_SIZE; i++) {
            preludeBuffer.writeByte(preludeChar);
        }
        preludeBuffer.writeByte('\n');
        PRELUDE_CHUNK = new DefaultHttpChunk(preludeBuffer);

        SEND_LAST_CHUNK = new SendLastChunk();
        SEND_ALREADY_CONNECTED = new SendCloseAlreadyConnected();
    }

    public XHttpRequest(SockJs sockJs) {
        super(sockJs);
        SEND_OPEN = new SendOpen();
    }


    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        log.info("Handling new request: " + httpRequest.getUri());
        if (httpRequest.getMethod() == HttpMethod.POST) {
            if (httpRequest.getUri().endsWith("/xhr_streaming")) {
                handleStreaming(ctx, httpRequest);
            } else if (httpRequest.getUri().endsWith("/xhr_send")) {
                handleSend(ctx, httpRequest);
            }
        } else if (httpRequest.getMethod() == HttpMethod.OPTIONS) {
                HttpHelpers.sendOptions(ctx, httpRequest, "OPTIONS, POST");
        }
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        connection.getChannel().write(HEARTBEAT_CHUNK);
    }

    @Override
    public void sendMessage(Connection connection, String[] messagesToSend) {
        String message = Protocol.encodeMessageToString(messagesToSend);
        ChannelBuffer content = ChannelBuffers.copiedBuffer(message + "\n", CharsetUtil.UTF_8);
        ChannelFuture writeFuture = connection.getChannel().write(new DefaultHttpChunk(content));
        connection.incSentBytes(content.readableBytes());

        if (connection.getSentBytes() > getSockJs().getMaxStreamSize()) {
            connection.resetSentBytes();
            writeFuture.addListener(SEND_LAST_CHUNK);
        }

    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().write(reason.httpChunk).addListener(SEND_LAST_CHUNK);
    }

    private void handleStreaming(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            HttpResponse response = createStreamingResponse(httpRequest);
            HttpHelpers.addJESSIONID(response, sockJsHandlerContext.getJSESSIONID());
            ChannelFutureListener nextListener;
            if (sockJsHandlerContext.getConnection() == null ||
                !sockJsHandlerContext.getConnection().getChannel().isWritable()) {
                nextListener = SEND_OPEN;
            } else {
                nextListener = SEND_ALREADY_CONNECTED;
            }

            ctx.getChannel().write(response).addListener(new SendPreludeListener(nextListener));
        } else {
            HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Internal Server Error");
        }
    }

    private void handleSend(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx.getChannel());
        String jsessionId = "dummy";
        if (sockJsHandlerContext != null) {
            String message = httpRequest.getContent().toString(CharsetUtil.UTF_8);
            log.info("Message received: " + message);
            Connection connection = sockJsHandlerContext.getConnection();
            if (connection != null) {
                try {
                    String[] messages = Protocol.decodeMessage(message);
                    if (messages != null) {
                        for (String decodedMessage : messages) {
                            connection.sendToListeners(decodedMessage);
                        }
                    } else {
                        HttpHelpers
                                .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                        "Payload expected.");
                    }

                }catch (JsonParseException ex) {
                    HttpHelpers
                            .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                    "Broken JSON encoding.");
                }
            } else {
                HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_FOUND);
                return;
            }
        }

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.NO_CONTENT);
        HttpHelpers.addJESSIONID(response, jsessionId);
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

        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    protected static HttpResponse createStreamingResponse(HttpRequest httpRequest) {
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
        if (httpRequest.getProtocolVersion() == HttpVersion.HTTP_1_1) {
            response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING, HttpHeaders.Values.CHUNKED);
        }
        response.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript;charset=UTF-8");

        return response;
    }

    private class SendPreludeListener implements ChannelFutureListener {

        private ChannelFutureListener nextListener;

        private SendPreludeListener(ChannelFutureListener nextListener) {
            this.nextListener = nextListener;
        }

        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            log.info("Sending prelude");
            future.getChannel().write(PRELUDE_CHUNK).addListener(nextListener);
        }
    }

    private class SendOpen implements ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            log.info("Sending open");
            HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers
                    .copiedBuffer("o\n", CharsetUtil.UTF_8));

            SockJsHandlerContext sockJsHandlerContext = XHttpRequest.this
                    .getSockJsHandlerContext(future.getChannel());
            if (sockJsHandlerContext != null) {
                Connection connection = sockJsHandlerContext.getConnection();
                if (connection == null) {
                    connection = XHttpRequest.this.getSockJs().createConnection(sockJsHandlerContext);
                }
                connection.setJSESSIONID(sockJsHandlerContext.getJSESSIONID());
                connection.setChannel(future.getChannel());

                final Connection newConnection = connection;
                future.getChannel().write(chunk).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future)
                            throws Exception {
                        XHttpRequest.this.getSockJs()
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

    private static class SendCloseAlreadyConnected implements  ChannelFutureListener {
        @Override
        public void operationComplete(ChannelFuture future)
                throws Exception {
            future.getChannel().write(Protocol.CloseReason.ALREADY_OPENED.httpChunk).addListener(SEND_LAST_CHUNK);
        }
    }
}
