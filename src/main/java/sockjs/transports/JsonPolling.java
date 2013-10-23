/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.Connection;
import sockjs.SockJs;
import sockjs.netty.*;

import java.net.URLDecoder;
import java.util.List;

public class JsonPolling extends XHttpRequestPolling {

    private static final Logger log = LoggerFactory.getLogger(JsonPolling.class);

    private static final String JSONP_OPEN_FRAME = "\"o\"";

    public JsonPolling(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        if (httpRequest.getMethod() == HttpMethod.GET) {
            pollMessages(ctx, httpRequest);
        } else if (httpRequest.getMethod() == HttpMethod.POST) {
            handleJsonPollingSend(ctx, httpRequest);
        }
    }

    @Override
    protected String encodeMessage(Connection connection, String[] messagesToSend) {
        String encodedMessage;
        if (!messagesToSend[0].equals(Protocol.OPEN_FRAME)) {
            encodedMessage = Protocol.encodeToJSONString(messagesToSend);
        } else {
            encodedMessage = JSONP_OPEN_FRAME;
        }
        return connection.getJsonpCallback() + "(" + encodedMessage + ");\r\n";
    }

    @Override
    public void handleCloseRequest(Connection connection, Protocol.CloseReason reason) {
        if (connection.getChannel().isWritable()) {
            ChannelBuffer content = ChannelBuffers.copiedBuffer(Protocol
                    .encodeJsonpClose(reason, connection.getJsonpCallback()), CharsetUtil.UTF_8);

            connection.getChannel().write(new DefaultHttpChunk(content)).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private void handleJsonPollingSend(ChannelHandlerContext ctx, HttpRequest httpRequest) {
        SockJsHandlerContext sockJsHandlerContext = getSockJsHandlerContext(ctx.getChannel());
        if (sockJsHandlerContext != null) {
            String message = httpRequest.getContent().toString(CharsetUtil.UTF_8);
            if (message.startsWith("d=")) {
                try {
                    message = message.length() >= 5 ? URLDecoder.decode(message.substring(2), "UTF-8"): "";
                } catch (Exception e) {
                    HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Payload expected.");
                    return;
                }
            } else if (!message.isEmpty() && !(message.charAt(0) == '[' || message.charAt(0) == '\"')) {
                message = "";
            }

            if (message.trim().isEmpty()) {
                HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Payload expected.");
                return;
            }

            log.info("Message received: " + message);
            Connection connection = sockJsHandlerContext.getConnection();
            if (connection != null) {
                try {
                    String[] messages = Protocol.decodeMessage(message);
                    if (messages != null) {
                        HttpResponse response = createResponse(ChannelBuffers.copiedBuffer("ok", CharsetUtil.UTF_8));
                        HttpHelpers.addJESSIONID(response, connection.getJSESSIONID());
                        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain;charset=UTF-8");
                        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
                        for (String decodedMessage : messages) {
                            connection.sendToListeners(decodedMessage);
                        }
                    } else {
                        HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Payload expected.");
                    }

                } catch (JsonParseException ex) {
                    HttpHelpers.sendError(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, "Broken JSON encoding.");
                }

            } else {
                HttpHelpers.sendError(ctx, HttpResponseStatus.NOT_FOUND);
            }
        }
    }

    @Override
    protected Connection createConnection(SockJsHandlerContext sockJsHandlerContext, HttpRequest
            httpRequest) {
        Connection connection = super.createConnection(sockJsHandlerContext, httpRequest);
        List<String> callbacks = new QueryStringDecoder(httpRequest.getUri()).getParameters().get("c");
        if (callbacks == null || callbacks.isEmpty()) {
            throw new HttpRequestException("\"callback\" parameter required",
                    HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        connection.setJsonpCallback(callbacks.get(0));

        return connection;
    }
}
