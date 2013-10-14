/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

public class Protocol {

    public static final String OPEN_FRAME = "o";

    public static final String HEARTBEAT_FRAME = "h";

    public static final String CLOSE_FRAME = "c";

    public static final String DATA_FRAME = "a";

    public static final TextWebSocketFrame WEB_SOCKET_OPEN_FRAME;

    public static final TextWebSocketFrame WEB_SOCKET_HEARTBEAT_FRAME;


    static  {
        WEB_SOCKET_HEARTBEAT_FRAME = new TextWebSocketFrame(HEARTBEAT_FRAME);
        WEB_SOCKET_OPEN_FRAME = new TextWebSocketFrame(OPEN_FRAME);
    }

}
