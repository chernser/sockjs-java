/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

public class WebSocketHandshakerFactory extends WebSocketServerHandshakerFactory {

    private final String webSocketURL;

    private final String subprotocols;

    private final boolean allowExtensions;

    private final long maxFramePayloadLength;

    public WebSocketHandshakerFactory(String webSocketURL, String subprotocols, boolean
            allowExtensions) {
        this(webSocketURL, subprotocols, allowExtensions, Long.MAX_VALUE);
    }

    public WebSocketHandshakerFactory(String webSocketURL, String subprotocols, boolean
            allowExtensions, long maxFramePayloadLength) {
        super(webSocketURL, subprotocols, allowExtensions, maxFramePayloadLength);
        this.webSocketURL = webSocketURL;
        this.subprotocols = subprotocols;
        this.allowExtensions = allowExtensions;
        this.maxFramePayloadLength = maxFramePayloadLength;

    }

    @Override
    public WebSocketServerHandshaker newHandshaker(HttpRequest req) {
        String version = req.getHeader(HttpHeaders.Names.SEC_WEBSOCKET_VERSION);
        if (version == null) {
            return new WebSocketHixie76Handshaker(webSocketURL, subprotocols, maxFramePayloadLength);
        } else {
            return super.newHandshaker(req);
        }
    }
}
