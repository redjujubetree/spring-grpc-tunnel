package top.redjujubetree.grpc.tunnel.client.service;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * Service interface for generating heartbeat messages
 */
public interface HeartbeatService {
    /**
     * Generate a heartbeat message for the current client
     * 
     * @param clientId The ID of the client sending the heartbeat
     * @return TunnelMessage representing the heartbeat
     */
    Object generateHeartbeat(String clientId);

    Object buildHeartbeatPayload(String clientId);
}