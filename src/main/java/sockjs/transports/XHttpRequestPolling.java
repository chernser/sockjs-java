/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
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

    protected void pollMessages(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx);
        if (sockJsHandlerContext != null) {
            Connection connection = sockJsHandlerContext.getConnection();
            SockJsEvent sendEvent;
            if (connection == null) {
                try {
                    connection = createConnection(sockJsHandlerContext, httpRequest);
                } catch (HttpRequestException ex) {
                    HttpHelpers.sendError(ctx, ex.getResponseStatus(), ex.getMessage());
                }

                sockJsHandlerContext.setConnection(connection);
                connection.addMessageToBuffer(OPEN_FRAME);
                connection.setChannel(ctx.getChannel());
                sendEvent = new SockJsSendEvent(connection);
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
                sendEvent = new SockJsSendEvent(connection);
            }

            ctx.getPipeline().sendUpstream(new UpstreamMessageEvent(ctx.getChannel(),
                    sendEvent, ctx.getChannel().getRemoteAddress()));
        } else {
            HttpHelpers
                    .sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR,
                            "Internal Server Error");
        }
    }

    protected Connection createConnection(SockJsHandlerContext sockJsHandlerContext,
                                          HttpRequest httpRequest) {

        Connection connection = getSockJs().createConnection(sockJsHandlerContext);
        connection.setJSESSIONID(sockJsHandlerContext.getJSESSIONID());
        return connection;
    }

    @Override
    public void sendMessage(Connection connection, String[] messagesToSend) {
        String encodedMessage = encodeMessage(connection, messagesToSend);
        ChannelBuffer content = ChannelBuffers.copiedBuffer(encodedMessage, CharsetUtil.UTF_8);
        HttpResponse response = createResponse(content);
        HttpHelpers.addJESSIONID(response, connection.getJSESSIONID());
        if (connection.getChannel().isWritable()) {
            connection.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    protected String encodeMessage(Connection connection, String[] messagesToSend) {
        if (!messagesToSend[0].equals(Protocol.OPEN_FRAME)) {
            return Protocol.encodeMessageToString(messagesToSend) + '\n';
        } else {
            return messagesToSend[0] + '\n';
        }
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        connection.getChannel().close();
    }

    protected static HttpResponse createResponse(ChannelBuffer content) {
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
