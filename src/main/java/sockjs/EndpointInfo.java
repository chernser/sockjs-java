/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import java.util.Random;

public class EndpointInfo {
    private static final String INFO_FMT_STRING = //
            "{\"websocket\":%s, \"origins\":[\"*:*\"], \"cookie_needed\":%s, \"entropy\":%d}";

    private static final Random entropyRandom = new Random();

    private boolean webSocketEnabled = true;

    private boolean cookiesNeeded = false;

    public boolean isWebSocketEnabled() {
        return webSocketEnabled;
    }

    public void setWebSocketEnabled(boolean webSocketEnabled) {
        this.webSocketEnabled = webSocketEnabled;
    }

    public boolean isCookiesNeeded() {
        return cookiesNeeded;
    }

    public void setCookiesNeeded(boolean cookiesNeeded) {
        this.cookiesNeeded = cookiesNeeded;
    }

    @Override
    public String toString() {
        return String.format(INFO_FMT_STRING, isWebSocketEnabled(), isCookiesNeeded(), Math
                .abs(entropyRandom.nextInt()));
    }
}
