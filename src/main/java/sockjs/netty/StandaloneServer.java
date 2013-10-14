/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sockjs.ConnectionListener;
import sockjs.SockJs;

import java.net.InetSocketAddress;

public class StandaloneServer  {

    private static final Logger log = LoggerFactory.getLogger(StandaloneServer.class);

    private final SockJs sockJs;

    public StandaloneServer(SockJs sockJs) {
        this.sockJs = sockJs;
    }

    public void start() {

        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory());
        bootstrap.setPipelineFactory(new PipelineFactory(sockJs));

        bootstrap.bind(new InetSocketAddress(3002));
    }

    public void stop() {

    }

    public static void main(String[] args) {
        log.info("Starting standalone server ");

        SockJs sockJs = new SockJs();
        sockJs.addListener("/chat", new ConnectionListener() {
            @Override
            public void onOpen() {

            }

            @Override
            public void onClose() {

            }

            @Override
            public void onMessage() {

            }
        });
        StandaloneServer server = new StandaloneServer(sockJs);
        server.start();
    }
}
