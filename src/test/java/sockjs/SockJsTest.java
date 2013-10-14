/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs;

import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class SockJsTest {


    @Test
    public void checkIfListenerRegisteredForBaseUrl() {
        ConnectionListener listener = mock(ConnectionListener.class);
        SockJs sockJs = new SockJs();

        sockJs.addListener("/echo", listener);
        sockJs.addListener("/chat/", listener);

        assertTrue(sockJs.hasListenerForRoute("/echo/123/123"));
        assertFalse(sockJs.hasListenerForRoute("/echo32"));
        assertTrue(sockJs.hasListenerForRoute("/chat/123/123"));
        assertTrue(sockJs.hasListenerForRoute("/chat/"));
        assertFalse(sockJs.hasListenerForRoute("/chat3/123"));
        assertFalse(sockJs.hasListenerForRoute("/battle"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addingInvalidListener(){
        SockJs sockJs = new SockJs();
        sockJs.addListener("/123/123", null);
    }
}
