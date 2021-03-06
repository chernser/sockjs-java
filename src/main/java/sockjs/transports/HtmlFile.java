/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.apache.commons.io.IOUtils;
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
import sockjs.SockJs;
import sockjs.netty.*;
import sockjs.netty.events.SockJsCloseEvent;
import sockjs.netty.events.SockJsEvent;
import sockjs.netty.events.SockJsSendEvent;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class HtmlFile extends XHttpRequest {

    private static final Logger log = LoggerFactory.getLogger(HtmlFile.class);

    private static final String htmlTemplate;

    private static final String OPEN_FRAME = "\"o\"";

    static {
        try {
            URL htmlfileResource =  Thread.currentThread().getContextClassLoader().getResource("htmlfile.html");
            File htmlfile = new File(htmlfileResource.getPath());
            String head = IOUtils.toString(new FileInputStream(htmlfile));
            char[] spaces = new char[1024 - head.length() + 20];
            Arrays.fill(spaces, ' ');

            htmlTemplate = head + String.valueOf(spaces) + "\r\n\r\n";
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public HtmlFile(SockJs sockJs) {
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

    private void handleStreaming(final ChannelHandlerContext ctx, HttpRequest httpRequest) {
        List<String> callbacks = new QueryStringDecoder(httpRequest.getUri()).getParameters().get("c");
        if (callbacks == null || callbacks.isEmpty()) {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "\"callback\" parameter required");
            return;
        }

        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            Connection connection = sockJsHandlerContext.getConnection();
            final SockJsEvent sendEvent;
            if (connection == null) {
                connection = getSockJs().createConnection(sockJsHandlerContext);
                sockJsHandlerContext.setConnection(connection);
                connection.setJsonpCallback(callbacks.get(0));
                connection.setChannel(ctx.getChannel());
                connection.setJSESSIONID(sockJsHandlerContext.getJSESSIONID());
                connection.addMessageToBuffer(OPEN_FRAME);
                sendEvent = new SockJsSendEvent(connection);
            } else if (connection.getCloseReason() != null) {
                connection.setChannel(ctx.getChannel());
                sendEvent = new SockJsCloseEvent(connection, connection.getCloseReason());
            } else {
                connection.setChannel(ctx.getChannel());
                sendEvent = new SockJsSendEvent(connection);
            }

            final String prelude = String.format(htmlTemplate, callbacks.get(0));
            HttpResponse response = createStreamingResponse(httpRequest);
            HttpHelpers.addJESSIONID(response, sockJsHandlerContext.getJSESSIONID());
            ctx.getChannel().write(response).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future)
                        throws Exception {
                    HttpChunk preludeChunk = new DefaultHttpChunk(ChannelBuffers.copiedBuffer(prelude, CharsetUtil.UTF_8));

                    future.getChannel().write(preludeChunk).addListener(new ChannelFutureListener() {


                        @Override
                        public void operationComplete(ChannelFuture future)
                                throws Exception {
                            if (sendEvent != null) {
                                ctx.getPipeline().sendUpstream(new UpstreamMessageEvent(ctx.getChannel(),
                                        sendEvent, ctx.getChannel().getRemoteAddress()));
                            }
                        }
                    });

                }
            });


        } else {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error");
        }
    }

    @Override
    public void sendMessage(Connection connection, String[] messagesToSend) {
        String message;
        if (!OPEN_FRAME.equals(messagesToSend[0])) {
             message = Protocol.encodeToJSONString(messagesToSend);
        } else {
            message = messagesToSend[0];
        }

        String encodedMessage = "<script>\np(" + message + ");\n</script>\r\n";
        ChannelBuffer content = ChannelBuffers.copiedBuffer(encodedMessage, CharsetUtil.UTF_8);
        HttpChunk httpChunk = new DefaultHttpChunk(content);


        if (connection.getChannel().isWritable()) {
            connection.getChannel().write(httpChunk);
            connection.incSentBytes(content.readableBytes());

            if (connection.getSentBytes() > getSockJs().getMaxStreamSize()) {
                connection.resetSentBytes();
                connection.getChannel().write(HttpChunk.LAST_CHUNK).addListener(ChannelFutureListener.CLOSE);
                connection.setCloseReason(Protocol.CloseReason.NORMAL);
            }
        }

        if (OPEN_FRAME.equals(message)) {
            getSockJs().notifyListenersAboutNewConnection(connection);
        }
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        if (connection.getChannel().isWritable()) {
            ChannelBuffer content = ChannelBuffers.copiedBuffer(Protocol
                    .encodeJsonpClose(reason, connection.getJsonpCallback()), CharsetUtil.UTF_8);

            connection.getChannel().write(new DefaultHttpChunk(content)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected static HttpResponse createStreamingResponse(HttpRequest httpRequest) {
        HttpResponse response = XHttpRequest.createStreamingResponse(httpRequest);
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/html;charset=UTF-8");
        return response;
    }
}
