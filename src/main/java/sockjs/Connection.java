/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.UpstreamMessageEvent;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import sockjs.netty.events.SockJsCloseEvent;
import sockjs.netty.events.SockJsSendEvent;
import sockjs.transports.AbstractTransport;
import sockjs.transports.Protocol;

import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Connection {

    private Channel channel;

    private static HashedWheelTimer heartbeatTimer = new HashedWheelTimer();

    private boolean keepSendingHeartbeat = false;

    private volatile int heartbeatIntervalSec = 25;

    private String id;

    private final SockJs sockJs;

    private final String baseUrl;

    private AtomicInteger sentBytes;

    private String jsonpCallback = null;

    private ConcurrentLinkedQueue<String> messages;

    private Protocol.CloseReason closeReason;

    private String JSESSIONID;

    private static final String[] EMTPY_MESSAGE_ARRAY = new String[] {};

    public Connection(SockJs sockJs, String baseUrl) {
        this.sockJs = sockJs;
        this.baseUrl = baseUrl;
        this.id = UUID.randomUUID().toString();
        this.sentBytes = new AtomicInteger();
        this.messages = new ConcurrentLinkedQueue<String>();
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

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setHeartbeatIntervalSec(int heartbeatIntervalSec) {
        this.heartbeatIntervalSec = heartbeatIntervalSec;
    }

    public void startHeartbeat() {
        setKeepSendingHeartbeat(true);
        startHeartbeat(this);
    }

    public void addMessageToBuffer(String message) {
        messages.add(message);
    }

    public void sendToChannel(String message) {
        addMessageToBuffer(message);
        if (getChannel() != null && getChannel().isWritable()) {
            AbstractTransport.sendUpstream(getChannel(), new SockJsSendEvent(this));
        }
    }

    public void sendToListeners(String message) {
        sockJs.notifyListeners(this, message);
    }

    public int getSentBytes() {
        return sentBytes.intValue();
    }

    public void incSentBytes(int byValue) {
        sentBytes.addAndGet(byValue);
    }

    public void resetSentBytes() {
        sentBytes.set(0);
    }

    public String getJsonpCallback() {
        return jsonpCallback;
    }

    public void setJsonpCallback(String jsonpCallback) {
        this.jsonpCallback = jsonpCallback;
    }

    public String pollMessage() {
        return messages.poll();
    }

    public String[] pollAllMessages() {
        if (messages.isEmpty()) {
            return EMTPY_MESSAGE_ARRAY;
        }

        int numOfMessages = messages.size();
        String[] polledMessages = new String[numOfMessages];
        for (int i = 0; i < numOfMessages; i++) {
            polledMessages[i] = messages.poll();
        }

        return polledMessages;
    }

    public Protocol.CloseReason getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(Protocol.CloseReason closeReason) {
        this.closeReason = closeReason;
        getChannel().getPipeline()
                .sendUpstream(new UpstreamMessageEvent(getChannel(), new SockJsCloseEvent(this,
                        closeReason), getChannel()
                        .getRemoteAddress()));

    }

    public String getJSESSIONID() {
        return JSESSIONID;
    }

    public void setJSESSIONID(String JSESSIONID) {
        this.JSESSIONID = JSESSIONID;
    }

    private static void startHeartbeat(final Connection connection) {

        TimerTask timerTask = new TimerTask() {
            @Override
            public void run(Timeout timeout)
                    throws Exception {
                if (connection.isKeepSendingHeartbeat() && connection.getChannel().isWritable()) {
                    // TODO: send heartbeat event
                    heartbeatTimer.newTimeout(this, connection
                            .getHeartbeatIntervalSec(), TimeUnit.SECONDS);
                }
            }
        };

        heartbeatTimer.newTimeout(timerTask, connection.getHeartbeatIntervalSec(), TimeUnit.SECONDS);
    }
}
