package top.redjujubetree.grpc.tunnel.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * GRPC Client Tunnel properties configuration.
 * Supports multiple tunnel configurations for different gRPC clients.
 * 
 * This maps to the configuration structure:
 * grpc:
 *   client:
 *     {client-name}:
 *       tunnel:
 *         enabled: true
 *         ...
 */
@Data
@ConfigurationProperties(prefix = "grpc")
public class TunnelsProperties {

    /**
     * Map of client configurations
     * This will bind to grpc.client.{client-name}
     */
    private Map<String, ClientConfig> client = new HashMap<>();

    /**
     * Get tunnel configuration for a specific client
     */
    public TunnelProperties getTunnelConfig(String clientName) {
        ClientConfig config = client.get(clientName);
        return config != null ? config.getTunnel() : null;
    }

    /**
     * Check if tunnel is enabled for a specific client
     */
    public boolean isTunnelEnabled(String clientName) {
        TunnelProperties tunnelConfig = getTunnelConfig(clientName);
        return tunnelConfig != null && tunnelConfig.isEnabled();
    }

    /**
     * Client configuration that contains tunnel and other properties
     * This allows tunnel config to coexist with grpc-spring-boot-starter properties
     */
    @Data
    public static class ClientConfig {
        /**
         * Tunnel configuration for this client
         */
        private TunnelProperties tunnel;
        
        // Other properties from grpc-spring-boot-starter will be ignored
        // Spring Boot will bind them but we don't use them
    }

}