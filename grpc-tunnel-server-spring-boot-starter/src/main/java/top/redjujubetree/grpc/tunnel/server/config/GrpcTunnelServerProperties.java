package top.redjujubetree.grpc.tunnel.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "grpc.tunnel.server")
public class GrpcTunnelServerProperties {
    
    private boolean enabled = true;
    private long heartbeatTimeout = 60000; // heartbeat timeout in milliseconds
    private int maxClients = 1000; // maximum number of clients
    private boolean tlsEnabled = false; // whether to enable TLS
    private String certChainFile; // cert chain file path
    private String privateKeyFile; // private key file path
}