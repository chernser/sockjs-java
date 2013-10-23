/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import sockjs.Connection;
import sockjs.transports.Protocol;

public class SockJsSendEvent implements SockJsEvent {

    private final Connection connection;

    public SockJsSendEvent(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
    }
}
