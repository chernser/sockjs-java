/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class StandaloneServer  {

    private static final Logger log = LoggerFactory.getLogger(StandaloneServer.class);

    public void start() {

        ServerBootstrap bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory());
        bootstrap.setPipelineFactory(new PipelineFactory());

        bootstrap.bind(new InetSocketAddress(3002));
    }

    public void stop() {

    }

    public static void main(String[] args) {
        log.info("Starting standalone server ");

        StandaloneServer server = new StandaloneServer();
        server.start();
    }
}
