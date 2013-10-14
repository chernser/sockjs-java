/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import sockjs.transports.Protocol;

import java.util.concurrent.TimeUnit;

public class Connection {

    private Channel channel;

    private static HashedWheelTimer heartbeatTimer = new HashedWheelTimer();

    private boolean keepSendingHeartbeat = false;

    private volatile int heartbeatIntervalSec = 25;

    public Connection(final Channel channel) {
        this.channel = channel;
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

    public void setHeartbeatIntervalSec(int heartbeatIntervalSec) {
        this.heartbeatIntervalSec = heartbeatIntervalSec;
    }

    public void startHeartbeat(Transport transport) {
        setKeepSendingHeartbeat(true);
        startHeartbeat(this, transport);
    }

    private static void startHeartbeat(final Connection connection, final Transport transport) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout)
                    throws Exception {
                if (connection.isKeepSendingHeartbeat()) {
                    transport.sendHeartbeat(connection.channel);
                    heartbeatTimer.newTimeout(this, connection
                            .getHeartbeatIntervalSec(), TimeUnit.SECONDS);
                }
            }
        };

        heartbeatTimer.newTimeout(timerTask, connection.getHeartbeatIntervalSec(), TimeUnit.SECONDS);

    }
}
