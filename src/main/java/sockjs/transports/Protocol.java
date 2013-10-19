/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import sockjs.Message;

import java.util.Collection;

public class Protocol {

    public static final String OPEN_FRAME = "o";

    public static final String HEARTBEAT_FRAME = "h";

    public static final String DATA_FRAME = "a";

    public static final int PRELUDE_SIZE = 2048; // 2KiB

    public static final TextWebSocketFrame WEB_SOCKET_OPEN_FRAME;

    public static final TextWebSocketFrame WEB_SOCKET_HEARTBEAT_FRAME;

    private static final ObjectMapper jsonObjectMapper;

    public enum CloseReason {
        NORMAL("c[3000,\"Go away!\"]"),
        ALREADY_OPENED("c[2010,\"Another connection still open\"]"),
        INTERRUPTED("c[1002,\"Connection interrupted\"]");

        public final String frame;

        public final TextWebSocketFrame webSocketFrame;

        public final HttpChunk httpChunk;

        private CloseReason(String frame) {
            this.frame = frame;
            webSocketFrame = new TextWebSocketFrame(frame);
            httpChunk = new DefaultHttpChunk(ChannelBuffers.copiedBuffer(frame + "\n", CharsetUtil.UTF_8));
        }
    }

    static {
        WEB_SOCKET_HEARTBEAT_FRAME = new TextWebSocketFrame(HEARTBEAT_FRAME);
        WEB_SOCKET_OPEN_FRAME = new TextWebSocketFrame(OPEN_FRAME);
        jsonObjectMapper = new ObjectMapper();
    }

    public static String encodeMessagesToString(Collection<Message> messages) {
        try {
            String encodedPayload = jsonObjectMapper.writeValueAsString(messages);
            return String.format("a[%s]", encodedPayload);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeMessageToString(String payload) {
        try {
            String encodedPayload = jsonObjectMapper.writeValueAsString(payload);
            return String.format("a[%s]", encodedPayload);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeToJSONString(String payload) {
        try {
            return jsonObjectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeToJSONString(String[] payload) {
        try {
            String encodedArray= jsonObjectMapper.writeValueAsString(payload);

            String encodedPayload =  jsonObjectMapper.writeValueAsString(String.format("a%s", encodedArray));
            return encodedPayload;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static String encodeMessageToString(String[] payload) {
            try {
                String encodedPayload = jsonObjectMapper.writeValueAsString(payload);
                return String.format("a%s", encodedPayload);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
    }

    public static String encodeJsonpClose(CloseReason reason, String callback) {
        try {
            String encodedPayload = jsonObjectMapper.writeValueAsString(reason.frame);
            return callback + String.format("(%s);\r\n\r\n", encodedPayload);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static TextWebSocketFrame encodeMessageToWebSocketFrame(String[] message) {
        return new TextWebSocketFrame(encodeMessageToString(message));
    }

    public static String[] decodeMessage(String message) {
        String[] messages = null;
        try {
            if (message.startsWith("[")) {
                return jsonObjectMapper.readValue(message, String[].class);
            } else if (message.startsWith("\"")) {
                String payload = jsonObjectMapper.readValue(message, String.class);
                return new String[] {payload};
            }
            return messages;
        } catch (JsonParseException ex) {
            return null;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
