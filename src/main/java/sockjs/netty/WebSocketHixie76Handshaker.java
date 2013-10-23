package sockjs.netty;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.*;
import org.jboss.netty.handler.codec.http.websocketx.*;

import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.WEBSOCKET_PROTOCOL;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.WEBSOCKET;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * @author Sergey Chernov
 *         See LICENSE file in the root of the project
 */
public class WebSocketHixie76Handshaker extends WebSocketServerHandshaker {

    public WebSocketHixie76Handshaker(String webSocketUrl, String subprotocols) {
        super(WebSocketVersion.V00, webSocketUrl, subprotocols);
    }

    public WebSocketHixie76Handshaker(String webSocketUrl, String subprotocols,
                                      long maxFramePayloadLength) {
        super(WebSocketVersion.V00, webSocketUrl, subprotocols, maxFramePayloadLength);
    }

    @Override
    public ChannelFuture handshake(Channel channel, HttpRequest req) {

        // Serve the WebSocket handshake request.
        if (!HttpHeaders.Values.UPGRADE.equalsIgnoreCase(req.getHeader(CONNECTION))
                || !WEBSOCKET.equalsIgnoreCase(req.getHeader(HttpHeaders.Names.UPGRADE))) {
            throw new WebSocketHandshakeException("not a WebSocket handshake request: missing upgrade");
        }

        // Hixie 75 does not contain these headers while Hixie 76 does
        boolean isHixie76 = req.containsHeader(SEC_WEBSOCKET_KEY1) && req.containsHeader(SEC_WEBSOCKET_KEY2);

        // Create the WebSocket handshake response.
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, new HttpResponseStatus(101,
                isHixie76 ? "WebSocket Protocol Handshake" : "Web Socket Protocol Handshake"));
        res.addHeader(HttpHeaders.Names.UPGRADE, WEBSOCKET);
        res.addHeader(CONNECTION, HttpHeaders.Values.UPGRADE);

        // Fill in the headers and contents depending on handshake method.
        if (isHixie76) {
            // New handshake method with a challenge:
            res.addHeader(SEC_WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
            res.addHeader(SEC_WEBSOCKET_LOCATION, getWebSocketUrl());
            String subprotocols = req.getHeader(SEC_WEBSOCKET_PROTOCOL);
            if (subprotocols != null) {
                String selectedSubprotocol = selectSubprotocol(subprotocols);
                if (selectedSubprotocol == null) {
                    throw new WebSocketHandshakeException("Requested subprotocol(s) not supported: " + subprotocols);
                } else {
                    res.addHeader(SEC_WEBSOCKET_PROTOCOL, selectedSubprotocol);
                    setSelectedSubprotocol(selectedSubprotocol);
                }
            }

            // Calculate the answer of the challenge.
            String key1 = req.getHeader(SEC_WEBSOCKET_KEY1);
            String key2 = req.getHeader(SEC_WEBSOCKET_KEY2);
            int a = (int) (Long.parseLong(key1.replaceAll("[^0-9]", "")) / key1.replaceAll("[^ ]", "").length());
            int b = (int) (Long.parseLong(key2.replaceAll("[^0-9]", "")) / key2.replaceAll("[^ ]", "").length());
            long c = req.getContent().readLong();
            ChannelBuffer input = ChannelBuffers.buffer(16);
            input.writeInt(a);
            input.writeInt(b);
            input.writeLong(c);
            res.setContent(md5(input));
        } else {
            // Old Hixie 75 handshake method with no challenge:
            res.addHeader(WEBSOCKET_ORIGIN, req.getHeader(ORIGIN));
            res.addHeader(WEBSOCKET_LOCATION, getWebSocketUrl());
            String protocol = req.getHeader(WEBSOCKET_PROTOCOL);
            if (protocol != null) {
                res.addHeader(WEBSOCKET_PROTOCOL, selectSubprotocol(protocol));
            }
        }

        ChannelFuture future = channel.write(res);

        // Upgrade the connection and send the handshake response.
        future.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture future) {
                ChannelPipeline p = future.getChannel().getPipeline();
                if (p.get(HttpChunkAggregator.class) != null) {
                    p.remove(HttpChunkAggregator.class);
                }
                p.get(HttpRequestDecoder.class).replace("wsdecoder",
                        new WebSocket00FrameDecoder(getMaxFramePayloadLength()));

                p.replace(HttpResponseEncoder.class, "wsencoder", new WebSocket00FrameEncoder());
            }
        });

        return future;
    }

    @Override
    public ChannelFuture close(Channel channel, CloseWebSocketFrame frame) {
        return channel.close();
    }

    private ChannelBuffer md5(ChannelBuffer input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (input.hasArray()) {
                md.update(input.array(), input.readerIndex(), input.readableBytes());
            } else {
                md.update(input.toByteBuffer());
            }
            return ChannelBuffers.wrappedBuffer(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new InternalError("MD5 not supported on this platform");
        }

    }
}
