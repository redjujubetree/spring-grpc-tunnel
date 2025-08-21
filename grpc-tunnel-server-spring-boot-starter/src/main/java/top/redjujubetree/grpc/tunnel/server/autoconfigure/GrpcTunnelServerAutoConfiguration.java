package top.redjujubetree.grpc.tunnel.server.autoconfigure;

import net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import top.redjujubetree.grpc.tunnel.handler.ConnectedHandler;
import top.redjujubetree.grpc.tunnel.handler.DisconnectedHandler;
import top.redjujubetree.grpc.tunnel.handler.HeartbeatHandler;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.server.GrpcTunnelServerService;
import top.redjujubetree.grpc.tunnel.server.config.GrpcTunnelServerProperties;
import top.redjujubetree.grpc.tunnel.server.handler.*;

import java.util.List;

@Configuration
@EnableConfigurationProperties(GrpcTunnelServerProperties.class)
@ConditionalOnClass(GrpcServerAutoConfiguration.class)
@AutoConfigureAfter(GrpcServerAutoConfiguration.class)
@ConditionalOnProperty(prefix = "grpc.tunnel.server", name = "enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = "com.redjujubetree.grpc.tunnel.server")
public class GrpcTunnelServerAutoConfiguration {

    @Bean
    public BasicConnectionHandler basicConnectionHandler() {
        return new BasicConnectionHandler();
    }
    @Bean
    @ConditionalOnMissingBean(HeartbeatHandler.class)
    public HeartbeatHandler defaultHeartbeatHandler() {
        return new DefaultHeartbeatHandler();
    }

    @Bean
    @ConditionalOnMissingBean(ConnectedHandler.class)
    public ConnectedHandler defaultConnectedHandler() {
        return new DefaultConnectedHandler();
    }

    @Bean
    @ConditionalOnMissingBean(DisconnectedHandler.class)
    public DisconnectedHandler defaultDisconnectedHandler() {
        return new DefaultDisconnectedHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    public GrpcTunnelServerService grpcTunnelServerService(
            GrpcTunnelServerProperties properties,
            List<ConnectionHandler> connectionHandlers,
            List<MessageHandler> messageHandlers,
            HeartbeatHandler heartbeatHandler) {
        return new GrpcTunnelServerService(
            properties,
            connectionHandlers,
            messageHandlers,
            heartbeatHandler
        );
    }
}