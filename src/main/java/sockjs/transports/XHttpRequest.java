/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
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
import sockjs.Transport;
import sockjs.netty.SockJsHandlerContext;

public class XHttpRequest extends AbstractTransport {

    private static final Logger log = LoggerFactory.getLogger(XHttpRequest.class);

    private static final HttpChunk HEARTBEAT_CHUNK;


    static {
        ChannelBuffer heartbeatContent = ChannelBuffers
                .copiedBuffer(Protocol.HEARTBEAT_FRAME + "\n", CharsetUtil.UTF_8);
        HEARTBEAT_CHUNK = new DefaultHttpChunk(heartbeatContent);

    }

    public XHttpRequest(SockJs sockJs) {
        super(sockJs);
    }


    @Override
    public void handle(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        log.info("Handling new request: " + httpRequest.getUri());
        if (httpRequest.getMethod() == HttpMethod.POST) {
            if (httpRequest.getUri().endsWith("/xhr_streaming")) {
                handleStreaming(ctx, httpRequest);
            } else if (httpRequest.getUri().endsWith("/xhr_send")) {
                handleSend(ctx, httpRequest);
            }
        }
    }

    @Override
    public void sendHeartbeat(Channel channel) {
        channel.write(HEARTBEAT_CHUNK);
    }

    @Override
    public void sendMessage(Channel channel, Message message) {
        ChannelBuffer content = ChannelBuffers
                .copiedBuffer(Protocol.encodeMessageToString(message) + "\n", CharsetUtil.UTF_8);
        channel.write(new DefaultHttpChunk(content));
        log.info("Message sent: " + message.getPayload());
    }

    private void handleStreaming(ChannelHandlerContext ctx, HttpRequest httpRequest) {
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
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript; charset=UTF-8");

        ctx.getChannel().write(response).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                HttpChunk chunk = new DefaultHttpChunk(ChannelBuffers
                        .copiedBuffer("o\n", CharsetUtil.UTF_8));
                future.getChannel().write(chunk);

                SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(future
                        .getChannel());
                if (sockJsHandlerContext != null) {
                    Connection connection = sockJsHandlerContext.getConnection();
                    if (connection == null) {
                        connection = getSockJs()
                                .createConnection(sockJsHandlerContext.getBaseUrl(),
                                                  sockJsHandlerContext.getSessionId());
                    }

                    connection.setChannel(future.getChannel());
                    connection.setTransport(XHttpRequest.this);
                }
            }
        });
    }

    private void handleSend(ChannelHandlerContext ctx, HttpRequest httpRequest) {
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
            }
        }

        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT);
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
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");

        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }
}
