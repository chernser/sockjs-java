/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Connection {

    private Channel channel;

    private Transport transport;

    private static HashedWheelTimer heartbeatTimer = new HashedWheelTimer();

    private boolean keepSendingHeartbeat = false;

    private volatile int heartbeatIntervalSec = 25;

    private String id;

    private final SockJs sockJs;

    private final String baseUrl;

    public Connection(SockJs sockJs, String baseUrl) {
        this.sockJs = sockJs;
        this.baseUrl = baseUrl;
        this.id = UUID.randomUUID().toString();
    }

    public void setChannel(Channel channel) {
        this.channel = channel;
    }

    public Channel getChannel() {
        return channel;
    }

    public boolean isKeepSendingHeartbeat() {
        return keepSendingHeartbeat;
    }

    public void setKeepSendingHeartbeat(boolean keepSendingHeartbeat) {
        this.keepSendingHeartbeat = keepSendingHeartbeat;
    }

    public int getHeartbeatIntervalSec() {
        return heartbeatIntervalSec;
    }

    public String getId() {
        return id;
    }

    public Transport getTransport() {
        return transport;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setHeartbeatIntervalSec(int heartbeatIntervalSec) {
        this.heartbeatIntervalSec = heartbeatIntervalSec;
    }

    public void startHeartbeat() {
        setKeepSendingHeartbeat(true);
        startHeartbeat(this, transport);
    }

    public void sendToChannel(Message message) {
        transport.sendMessage(getChannel(), message);
    }

    public void sendToListeners(Message message) {
        sockJs.notifyListeners(this, message);
    }

    private static void startHeartbeat(final Connection connection, final Transport transport) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout)
                    throws Exception {
                if (connection.isKeepSendingHeartbeat() && connection.getChannel().isWritable()) {
                    transport.sendHeartbeat(connection.getChannel());
                    heartbeatTimer.newTimeout(this, connection
                            .getHeartbeatIntervalSec(), TimeUnit.SECONDS);
                }
            }
        };

        heartbeatTimer.newTimeout(timerTask, connection.getHeartbeatIntervalSec(), TimeUnit.SECONDS);
    }
}
