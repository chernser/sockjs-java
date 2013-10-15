/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.jboss.netty.channel.Channel;
import sockjs.transports.WebSocket;
import sockjs.transports.XHttpRequest;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SockJs {

    public static final String WEBSOCKET_TRANSPORT = "websocket";

    public static final String XHR_TRANSPORT = "xhr";

    public static final String XHR_TRANSPORT_SEND = "xhr_send";

    public static final String XHR_TRANSPORT_STREAMING = "xhr_streaming";

    private ConcurrentHashMap<String, Set<ConnectionListener>> listeners;

    private static final String INFO_FMT_STRING = //
            "{\"websocket\":%s, \"origins\":[\"*:*\"], \"cookie_needed\":%s, \"entropy\":%d}";

    private static final Random entropyRandom = new Random();

    private boolean isWebSocketEnabled = true;

    private boolean isCookiesNeeded = false;

    private Map<String, Transport> transports;

    public SockJs() {
        listeners = new ConcurrentHashMap<String, Set<ConnectionListener>>();
        transports = new HashMap<String, Transport>();

        addTransport(WEBSOCKET_TRANSPORT, new WebSocket(this));
        XHttpRequest xhr = new XHttpRequest(this);
        addTransport(XHR_TRANSPORT, xhr);
        addTransport(XHR_TRANSPORT_SEND, xhr);
        addTransport(XHR_TRANSPORT_STREAMING, xhr);
    }

    public boolean isWebSocketEnabled() {
        return isWebSocketEnabled;
    }

    public void setWebSocketEnabled(boolean webSocketEnabled) {
        isWebSocketEnabled = webSocketEnabled;
    }

    public boolean isCookiesNeeded() {
        return isCookiesNeeded;
    }

    public void setCookiesNeeded(boolean cookiesNeeded) {
        isCookiesNeeded = cookiesNeeded;
    }

    public void addListener(String baseUrl, ConnectionListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        baseUrl = normalizeBaseUrl(baseUrl);

        Set<ConnectionListener> baseUrlListeners = listeners.get(baseUrl);
        if (baseUrlListeners == null) {
            baseUrlListeners = new HashSet<ConnectionListener>();
            Set<ConnectionListener> oldSet = listeners.putIfAbsent(baseUrl, baseUrlListeners);
            baseUrlListeners = oldSet == null ? baseUrlListeners : oldSet;
        }

        baseUrlListeners.add(listener);
    }

    public void removeAllListeners(String baseUrl) {
        baseUrl = normalizeBaseUrl(baseUrl);
        listeners.remove(baseUrl);
    }

    public void removeListener(String baseUrl, ConnectionListener listener) {
        baseUrl = normalizeBaseUrl(baseUrl);

        Set<ConnectionListener> baseUrlListeners = listeners.get(baseUrl);
        if (baseUrlListeners != null) {
            baseUrlListeners.remove(listener);
        }
    }

    public boolean hasListenerForRoute(String url) {

        for (String baseUrl : listeners.keySet()) {
            if (url.indexOf(baseUrl) == 0) {
                if (url.length() == baseUrl.length() || url.charAt(baseUrl.length()) == '/') {
                    return true;
                }
            }
        }

        return false;
    }

    public String getBaseUrl(String url) {
        for (String baseUrl : listeners.keySet()) {
            if (url.indexOf(baseUrl) == 0) {
                if (url.length() == baseUrl.length() || url.charAt(baseUrl.length()) == '/') {
                    return baseUrl;
                }
            }
        }

        return null;
    }

    public void notifyListeners(Connection connection, Message message) {
        Collection<ConnectionListener> connectionListeners = listeners.get(connection.getBaseUrl());

        if (connectionListeners != null) {
            for (ConnectionListener listener : connectionListeners) {
                listener.onMessage(connection, message);
            }
        }
    }

    public boolean isRootOfBaseUrl(String url) {
        return listeners.containsKey(url);
    }


    public String getInfoAsString() {
        return String.format(INFO_FMT_STRING, isWebSocketEnabled(), isCookiesNeeded(), Math
                .abs(entropyRandom.nextInt()));
    }

    public Transport getTransport(String shortName) {
        return transports.get(shortName);
    }

    public Connection createConnection(String baseUrl) {
        return new Connection(this, baseUrl);
    }

    private void addTransport(String shortName, Transport transport) {
        transports.put(shortName, transport);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
