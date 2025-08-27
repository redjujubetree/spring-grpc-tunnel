package top.redjujubetree.grpc.tunnel.server.handler;

import top.redjujubetree.grpc.tunnel.constant.ClientRequestTypes;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * heartbeat handler
 */
public interface HeartbeatHandler {
    /**
     * handle heartbeat
     */
    void handleHeartbeat(TunnelMessage message);

    default boolean support(TunnelMessage message) {
        return message.hasRequest() && ClientRequestTypes.HEARTBEAT.equals(message.getRequest().getType());
    }
    /**
     * handle timeout
     */
    void handleTimeout(String clientId);
}