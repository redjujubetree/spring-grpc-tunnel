package top.redjujubetree.grpc.tunnel.client.config;

import lombok.Data;

/**
 * GRPC Tunnel client properties configuration.
 */
@Data
public class TunnelProperties {

    private String clientId;
    private String clientName;
    /**
     * whether the client is enabled
     */
    private boolean enabled = false;

    /**
     * whether to automatically connect to the server on startup
     */
    private boolean autoConnect = true;

    /**
     * whether to automatically reconnect if the connection is lost
     */
    private boolean autoReconnect = true;

    /**
     * heartbeat timeout (milliseconds)
     */
    private long heartbeatInterval = 30000;

    /**
     * reconnect delay (milliseconds)
     */
    private long reconnectDelay = 5000;

    /**
     * max reconnect delay (milliseconds)
     */
    private long maxReconnectDelay = 300000; // 5 minutes

    /**
     * when true, the reconnect delay will increase exponentially up to maxReconnectDelay
     * default true
     */
    private boolean exponentialBackoff = true;

    /**
     * max reconnect attempts (-1 means unlimited)
     */
    private int maxReconnectAttempts = -1;

    /**
     * request timeout in milliseconds
     * default 30 seconds
     */
    private long requestTimeout = 30000;

}