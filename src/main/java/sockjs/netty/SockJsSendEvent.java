/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import sockjs.Connection;

public class SockJsSendEvent {

    private final Connection connection;

    private final String payload;

    public SockJsSendEvent(Connection connection, String payload) {
        this.connection = connection;
        this.payload = payload;
    }

    public Connection getConnection() {
        return connection;
    }

    public String getPayload() {
        return payload;
    }
}
