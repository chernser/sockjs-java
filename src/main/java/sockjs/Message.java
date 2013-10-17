/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

public class Message {

    private String payload;

    public Message(String payload) {
        this.payload = payload;
    }

    public String getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return payload;
    }
}
