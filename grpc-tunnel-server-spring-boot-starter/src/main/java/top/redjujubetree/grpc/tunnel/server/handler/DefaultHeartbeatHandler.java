package top.redjujubetree.grpc.tunnel.server.handler;


import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.redjujubetree.grpc.tunnel.constant.ClientRequestTypes;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * Default heartbeat handler for server-side processing.
 * This class can be extended to implement custom behavior for handling heartbeats.
 */
public class DefaultHeartbeatHandler implements HeartbeatHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultHeartbeatHandler.class);


    public boolean support(TunnelMessage message) {
        return message.hasRequest() && ClientRequestTypes.HEARTBEAT.equals(message.getRequest().getType());
    }

    @Override
    public void handleHeartbeat(TunnelMessage message) {
        log.trace("Processing default heartbeat from client: {}", message.getClientId());
        ByteString data = message.getRequest().getData();
        String payload = data.toStringUtf8();
        log.info(payload);
    }

    @Override
    public void handleTimeout(String clientId) {
        log.info("Heartbeat timeout for client: {}", clientId);
    }
}