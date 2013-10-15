/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.transports;

import org.jboss.netty.channel.Channel;
import sockjs.Message;
import sockjs.SockJs;
import sockjs.Transport;

public class XHttpRequest extends AbstractTransport {

    public XHttpRequest(SockJs sockJs) {
        super(sockJs);
    }

    @Override
    public void sendHeartbeat(Channel channel) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void sendMessage(Channel channel, Message message) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
