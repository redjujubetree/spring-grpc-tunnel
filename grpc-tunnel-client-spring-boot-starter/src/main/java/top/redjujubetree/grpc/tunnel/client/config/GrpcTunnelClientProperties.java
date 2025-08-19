package top.redjujubetree.grpc.tunnel.client.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * GRPC Tunnel client properties configuration.
 */
@Data
@ConfigurationProperties(prefix = "grpc.tunnel.client")
public class GrpcTunnelClientProperties {

    /**
     * whether the client is enabled
     */
    private boolean enabled = true;

    /**
     * Channel name (when specified, it will use the channel from the Spring container or GrpcChannelFactory)
     */
    private String channelName;

    /**
     * server address (used when channelName is empty
     */
    private String serverAddress = "localhost:9090";

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

    /**
     * weather to enable TLS
     */
    private boolean tlsEnabled = false;

    /**
     * trust certificate file path
     * when tlsEnabled is true, this file is used to verify the server's certificate
     */
    private String trustCertCollectionFile;

    /**
     * Keep-alive (milliseconds)
     */
    private long keepAliveTime = 30000;

    /**
     * Keep-alive timeout (milliseconds)
     */
    private long keepAliveTimeout = 10000;

    /**
     * send keep-alive enev if there are no calls
     */
    private boolean keepAliveWithoutCalls = true;

    /**
     * no activity timeout, after which the channel will be closed
     */
    private long idleTimeout = 300000; // 5分钟

    /**
     * max size of inbound messages in bytes
     */
    private int maxInboundMessageSize = 4 * 1024 * 1024; // 4MB

    /**
     * maximum size of metadata received from the server
     */
    private int maxInboundMetadataSize = 8192; // 8KB

    /**
     * whether to enable retry on failures
     */
    private boolean enableRetry = true;

    /**
     * maximum number of retry attempts
     */
    private int maxRetryAttempts = -1;

}