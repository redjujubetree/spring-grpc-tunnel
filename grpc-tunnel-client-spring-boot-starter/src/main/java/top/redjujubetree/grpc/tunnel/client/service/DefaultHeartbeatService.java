package top.redjujubetree.grpc.tunnel.client.service;


import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of HeartbeatService
 */
@Slf4j
public class DefaultHeartbeatService implements HeartbeatService {
    
    @Override
    public Object generateHeartbeat(String clientId) {
        return buildHeartbeatPayload(clientId);
    }

    /**
     * Build default heartbeat payload with system information
     */
    @Override
    public Object buildHeartbeatPayload(String clientId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("timestamp", System.currentTimeMillis());
        payload.put("clientId", clientId);
        payload.put("status", "alive");
        log.debug("heartbeat payload for client: {}, payload: {}", clientId, payload);
        return payload;
    }

}