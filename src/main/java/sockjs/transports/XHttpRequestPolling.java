/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;
import sockjs.netty.*;

import java.util.Collection;

public class XHttpRequestPolling extends AbstractTransport{

    private static final Logger log = LoggerFactory.getLogger(XHttpRequestPolling.class);

    private static final String OPEN_FRAME = "o";

    public XHttpRequestPolling(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        log.info("Handling new request: " + httpRequest.getUri());
        if (httpRequest.getMethod() == HttpMethod.POST) {
            if (httpRequest.getUri().endsWith("/xhr")) {
                pollMessages(ctx, httpRequest);
            }
        } else if (httpRequest.getMethod() == HttpMethod.OPTIONS) {
            HttpHelpers.sendOptions(ctx, httpRequest, "OPTIONS, POST");
            return;
        }

    }

    private void pollMessages(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            Connection connection = sockJsHandlerContext.getConnection();
            SockJsEvent sendEvent;
            if (connection == null) {
                connection = getSockJs().createConnection(sockJsHandlerContext);
                sockJsHandlerContext.setConnection(connection);
                connection.setChannel(ctx.getChannel());
                connection.setJSESSIONID(sockJsHandlerContext.getJSESSIONID());
                sendEvent = new SockJsSendEvent(connection, OPEN_FRAME, true);
            } else if (connection.getCloseReason() != null) {
                log.info("Connection is closed: " + connection.getCloseReason());
                connection.setChannel(ctx.getChannel());
                sendEvent = new SockJsCloseEvent(connection, connection.getCloseReason());
            } else if (connection.getChannel() != null && connection.getChannel().isWritable()) {
                ChannelBuffer content = ChannelBuffers.copiedBuffer(Protocol.CloseReason.ALREADY_OPENED.frame + "\n", CharsetUtil.UTF_8);
                ctx.getChannel().write(createResponse(content)).addListener(ChannelFutureListener.CLOSE);
                return;
            } else {
                log.info("polling all messages we have to send");
                connection.setChannel(ctx.getChannel());
                String[] messagesToSend = connection.pollAllMessages();
                if (messagesToSend.length > 0) {
                    String encodedMessage = Protocol.encodeMessageToString(messagesToSend);
                    sendEvent = new SockJsSendEvent(connection, encodedMessage, true);
                } else {
                    sendEvent = null;
                }
            }

            if (sendEvent != null) {
                ctx.getPipeline().sendUpstream(new UpstreamMessageEvent(ctx.getChannel(),
                        sendEvent, ctx.getChannel().getRemoteAddress()));
            }
        } else {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error");
        }
    }

    @Override
    public void sendHeartbeat(Connection connection) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendMessage(Connection connection, String encodedMessage) {
        ChannelBuffer content = ChannelBuffers.copiedBuffer(encodedMessage + '\n', CharsetUtil.UTF_8);
        HttpResponse response = createResponse(content);
        HttpHelpers.addJESSIONID(response, connection.getJSESSIONID());
        connection.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().close();
    }

    private static HttpResponse createResponse(ChannelBuffer content) {
        HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
                HttpResponseStatus.OK);
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.setHeader(HttpHeaders.Names.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "no-store, no-cache, must-revalidate," +
                "" + " max-age=0");
        response.setHeader(HttpHeaders.Names.CONNECTION, "keep-alive");
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/javascript;charset=UTF-8");
        response.setHeader(HttpHeaders.Names.CONTENT_LENGTH, content.readableBytes());

        response.setContent(content);
        return response;
    }


}
