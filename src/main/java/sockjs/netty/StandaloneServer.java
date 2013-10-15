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

    private ServerBootstrap bootstrap;

    private int port;

    public StandaloneServer(SockJs sockJs, int port) {
        this.sockJs = sockJs;
        this.port = port;
    }

    public synchronized void start() {
        stop();
        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory());
        bootstrap.setPipelineFactory(new PipelineFactory(sockJs));
        bootstrap.bind(new InetSocketAddress(getPort()));
    }

    public synchronized void stop() {
        if (getBootstrap() == null) {
            return;
        }

        ServerBootstrap currentBootstrap = getBootstrap();
        setBootstrap(null);

        currentBootstrap.shutdown();
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public ServerBootstrap getBootstrap() {
        return bootstrap;
    }

    private void setBootstrap(ServerBootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }
}
