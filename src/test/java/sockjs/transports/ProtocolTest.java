/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.junit.Test;
import sockjs.Message;

import static junit.framework.Assert.*;

public class ProtocolTest {

    @Test
    public void encodeMessage() {

        assertEquals("a[\"test\"]", Protocol.encodeMessageToString("test"));

        assertEquals("a[\"{\\\"value\\\": 123}\"]", Protocol.encodeMessageToString("test"));
    }

    @Test
    public void decodeMessage() throws Exception{
        String[] messages = Protocol.decodeMessage("[\"test\"]");
        assertEquals("test", messages[0]);

        messages = Protocol.decodeMessage("[\"value\", \"123\"]");
        assertEquals("value", messages[0]);
        assertEquals("123", messages[1]);
    }

    @Test
    public void encodeJSONPCallback() {
        String[] messages = new String[]{"123", "hello"};
        String encodedMessage = Protocol.encodeToJSONString(messages);

        System.out.println("encoded message: " + encodedMessage);
    }

}
