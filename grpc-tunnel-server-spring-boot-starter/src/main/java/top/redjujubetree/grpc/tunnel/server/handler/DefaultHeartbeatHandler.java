package top.redjujubetree.grpc.tunnel.server.handler;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.handler.HeartbeatHandler;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Default heartbeat handler for server-side processing
 */
@Component
public class DefaultHeartbeatHandler implements HeartbeatHandler {
    private static final Logger log = LoggerFactory.getLogger(DefaultHeartbeatHandler.class);


    public boolean supports(TunnelMessage message) {
        return message.hasRequest() && "HEARTBEAT".equals(message.getRequest().getType());
    }

    @Override
    public void handleHeartbeat(TunnelMessage message) {
        log.trace("Processing default heartbeat from client: {}", message.getClientId());
        ByteString data = message.getRequest().getData();
        String payload = data.toStringUtf8();
        log.info(payload);
    }

    /**
     * Simple JSON conversion method
     */
    private String convertToJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (i++ > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                json.append("\"").append(entry.getValue()).append("\"");
            } else {
                json.append(entry.getValue());
            }
        }
        json.append("}");
        return json.toString();
    }

    @Override
    public void handleTimeout(String clientId) {

    }
}