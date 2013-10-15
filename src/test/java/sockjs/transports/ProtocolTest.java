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

        Message msg1 = new Message("test");
        assertEquals("a[\"test\"]", Protocol.encodeMessageToString(msg1));

        Message msg2 = new Message("{\"value\": 123}");
        assertEquals("a[\"{\\\"value\\\": 123}\"]", Protocol.encodeMessageToString(msg2));
    }

    @Test
    public void decodeMessage() {
        Message[] messages = Protocol.decodeMessage("[\"test\"]");
        assertEquals("test", messages[0].getPayload());

        messages = Protocol.decodeMessage("[\"value\", \"123\"]");
        assertEquals("value", messages[0].getPayload());
        assertEquals("123", messages[1].getPayload());
    }
}
