package top.redjujubetree.grpc.tunnel.handler;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * heartbeat handler
 */
public interface HeartbeatHandler {
    /**
     * handle heartbeat
     */
    void handleHeartbeat(TunnelMessage message);

    default boolean supports(TunnelMessage message) {
        return message.hasRequest() && "HEARTBEAT".equals(message.getRequest().getType());
    }
    /**
     * handle timeout
     */
    void handleTimeout(String clientId);
}