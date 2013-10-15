/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.netty.StandaloneServer;

public class TestStandaloneServer {

    private static final Logger log = LoggerFactory.getLogger(TestStandaloneServer.class);

    public static void main(String[] args) {
        log.info("Starting standalone server ");

        SockJs sockJs = new SockJs();
        sockJs.addListener("/echo", new EchoListener());
        StandaloneServer server = new StandaloneServer(sockJs, 3002);
        server.start();
    }

    private static class EchoListener implements ConnectionListener {
        @Override
        public void onOpen(Connection connection) {
            log.info("new connection: " + connection.getId());
        }

        @Override
        public void onClose(Connection connection) {
            log.info("connection closed: " + connection.getId());
        }

        @Override
        public void onMessage(Connection connection, Message message) {
            log.info("message from connection: " + connection.getId() + " {" + message
                    .getPayload() + "}");
            connection.send(message);
        }
    }
}
