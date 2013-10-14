/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SockJs {

    private ConcurrentHashMap<String, Set<ConnectionListener>> listeners;

    private static final String INFO_FMT_STRING = //
            "{\"websocket\":%s, \"origins\":[\"*:*\"], \"cookie_needed\":%s, \"entropy\":%d}";

    private static final Random entropyRandom = new Random();

    private boolean isWebSocketEnabled = true;

    private boolean isCookiesNeeded = false;

    public SockJs() {
        listeners = new ConcurrentHashMap<String, Set<ConnectionListener>>();
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

    public boolean isRootOfBaseUrl(String url) {
        return listeners.containsKey(url);
    }

    public String getInfoAsString() {
        return String.format(INFO_FMT_STRING, isWebSocketEnabled(), isCookiesNeeded(), entropyRandom
                .nextInt());
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
