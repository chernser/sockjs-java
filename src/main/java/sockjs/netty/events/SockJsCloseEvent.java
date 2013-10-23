/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty.events;

import sockjs.Connection;
import sockjs.transports.Protocol;

public class SockJsCloseEvent implements SockJsEvent {

    private final Connection connection;

    private final Protocol.CloseReason reason;

    public SockJsCloseEvent(Connection connection, Protocol.CloseReason reason) {
        this.connection = connection;
        this.reason = reason;
    }

    public Connection getConnection() {
        return connection;
    }

    public Protocol.CloseReason getReason() {
        return reason;
    }
}
