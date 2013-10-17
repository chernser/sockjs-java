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
        sockJs.setMaxStreamSize(4096);
        EndpointInfo chatEndpointInfo = new EndpointInfo();
        chatEndpointInfo.setWebSocketEnabled(false);
        sockJs.setEndpointInfo("/chat", chatEndpointInfo);

        EndpointInfo disabledWebSocketEcho = new EndpointInfo();
        disabledWebSocketEcho.setWebSocketEnabled(false);
        sockJs.setEndpointInfo("/disabled_websocket_echo", disabledWebSocketEcho);

        EndpointInfo cookieNeededEcho = new EndpointInfo();
        cookieNeededEcho.setCookiesNeeded(true);
        sockJs.setEndpointInfo("/cookie_needed_echo", cookieNeededEcho);

        ConnectionListener connectionListener = new EchoListener();
        sockJs.addListener("/chat", connectionListener);
        sockJs.addListener("/echo", connectionListener);
        sockJs.addListener("/disabled_websocket_echo", connectionListener);
        sockJs.addListener("/cookie_needed_echo", connectionListener);
        sockJs.addListener("/close", new CloseListener());
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
            connection.sendToChannel(message);
        }
    }

    private static class CloseListener implements  ConnectionListener {
        @Override
        public void onOpen(Connection connection) {
            connection.close();
        }

        @Override
        public void onClose(Connection connection) {

        }

        @Override
        public void onMessage(Connection connection, Message message) {

        }
    }
}
