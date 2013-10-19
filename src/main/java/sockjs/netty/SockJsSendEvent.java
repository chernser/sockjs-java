/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import sockjs.Connection;
import sockjs.transports.Protocol;

public class SockJsSendEvent implements SockJsEvent {

    private final Connection connection;

    private final String payload;

    public SockJsSendEvent(Connection connection, String payload) {
        this(connection, payload, false);
    }

    public SockJsSendEvent(Connection connection, String payload, boolean raw) {
        this.connection = connection;
        this.payload = raw ? payload : Protocol.encodeMessageToString(payload);
    }

    public Connection getConnection() {
        return connection;
    }

    public String getPayload() {
        return payload;
    }
}
