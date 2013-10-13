/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
package sockjs.netty;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

public class PipelineFactory implements ChannelPipelineFactory {

    @Override
    public ChannelPipeline getPipeline()
            throws Exception {
        ChannelPipeline pipeline = new DefaultChannelPipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(Short.MAX_VALUE));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        pipeline.addLast("http_handler", new HttpRequestHandler());
        pipeline.addLast("ws_handler", new WebSocketFrameHandler());

        return pipeline;
    }
}
