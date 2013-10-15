/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import sockjs.Message;

public class Protocol {

    public static final String OPEN_FRAME = "o";

    public static final String HEARTBEAT_FRAME = "h";

    public static final String CLOSE_FRAME = "c";

    public static final String DATA_FRAME = "a";

    public static final TextWebSocketFrame WEB_SOCKET_OPEN_FRAME;

    public static final TextWebSocketFrame WEB_SOCKET_HEARTBEAT_FRAME;

    private static final ObjectMapper jsonObjectMapper;

    static {
        WEB_SOCKET_HEARTBEAT_FRAME = new TextWebSocketFrame(HEARTBEAT_FRAME);
        WEB_SOCKET_OPEN_FRAME = new TextWebSocketFrame(OPEN_FRAME);
        jsonObjectMapper = new ObjectMapper();
    }

    public static String encodeMessageToString(Message message) {
        try {
            String encodedPayload = jsonObjectMapper.writeValueAsString(message.getPayload());
            return String.format("a[%s]", encodedPayload);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static TextWebSocketFrame encodeMessageToWebSocketFrame(Message message) {
        return new TextWebSocketFrame(encodeMessageToString(message));
    }

    public static Message[] decodeMessage(String message) {
        Message[] messages = null;
        try {
            if (message.startsWith("[")) {
                String[] payloads = jsonObjectMapper.readValue(message, String[].class);
                messages = new Message[payloads.length];
                for (int i = 0; i < payloads.length; i++) {
                    messages[i] = new Message(payloads[i]);
                }
            } else if (message.startsWith("\"")) {
                String payload = jsonObjectMapper.readValue(message, String.class);
                messages = new Message[] { new Message(payload)};
            }
            return messages;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
