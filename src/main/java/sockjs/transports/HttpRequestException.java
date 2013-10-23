/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.handler.codec.http.HttpResponseStatus;

public class HttpRequestException extends RuntimeException {

    private HttpResponseStatus responseStatus;

    public HttpRequestException(String message, HttpResponseStatus responseStatus) {
        super(message);
        this.responseStatus = responseStatus;
    }

    public HttpResponseStatus getResponseStatus() {
        return responseStatus;
    }
}
