package top.redjujubetree.grpc.tunnel.client.autoconfigure;

import top.redjujubetree.grpc.tunnel.client.GrpcTunnelClientService;
import top.redjujubetree.grpc.tunnel.client.config.GrpcTunnelClientProperties;
import top.redjujubetree.grpc.tunnel.client.service.ClientInfoService;
import top.redjujubetree.grpc.tunnel.client.service.DefaultClientInfoService;
import top.redjujubetree.grpc.tunnel.client.service.DefaultHeartbeatService;
import top.redjujubetree.grpc.tunnel.client.service.HeartbeatService;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.generator.DefaultClientIdGenerator;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GRPC Tunnel Autoconfiguration for Client
 */
@Configuration
@EnableConfigurationProperties(GrpcTunnelClientProperties.class)
@ConditionalOnProperty(prefix = "grpc.tunnel.client", name = "enabled", havingValue = "true", matchIfMissing = true)
public class GrpcTunnelClientAutoConfiguration {
    
    @javax.annotation.Resource
    private ApplicationContext applicationContext;
    
    @Autowired(required = false)
    private GrpcChannelFactory grpcChannelFactory;
    
    /**
     * default ClientIdGenerator
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientIdGenerator clientIdGenerator() {
        return new DefaultClientIdGenerator();
    }
    
    /**
     * Create ManagedChannel
     * Prefer to use existing GrpcChannelFactory, otherwise create a new one
     */
    @Bean(name = "grpcTunnelChannel", destroyMethod = "shutdown")
    @ConditionalOnMissingBean(name = "grpcTunnelChannel")
    public ManagedChannel grpcTunnelChannel(GrpcTunnelClientProperties properties) {
        
        // Method 1: If a channel name is configured, try to get it from the Spring container or GrpcChannelFactory
        if (properties.getChannelName() != null) {
            // try to get the channel from the application context
            try {
                ManagedChannel existingChannel = applicationContext.getBean(properties.getChannelName(), ManagedChannel.class);
                if (existingChannel != null) {
                    return existingChannel;
                }
            } catch (Exception e) {
                // finding channel by name failed, fallback to create a new one
            }
            
            // try to get the channel from GrpcChannelFactory
            if (grpcChannelFactory != null) {
                try {
                    return (ManagedChannel) grpcChannelFactory.createChannel(properties.getChannelName());
                } catch (Exception e) {
                    // GrpcChannelFactory creation failed, fallback to create a new one
                }
            }
        }
        
        // Method 2: If no channel name is configured or the above methods failed, create a new ManagedChannel
        return createManagedChannel(properties);
    }
    
    /**
     * create a new ManagedChannel based on the properties
     */
    private ManagedChannel createManagedChannel(GrpcTunnelClientProperties properties) {
        ManagedChannelBuilder<?> builder;
        
        // if TLS is enabled, use NettyChannelBuilder with SSL context
        if (properties.isTlsEnabled()) {
            NettyChannelBuilder nettyBuilder = NettyChannelBuilder.forTarget(properties.getServerAddress());
            
            try {
                if (properties.getTrustCertCollectionFile() != null) {
                    File trustCertFile = getResourceFile(properties.getTrustCertCollectionFile());
                    nettyBuilder.sslContext(GrpcSslContexts.forClient()
                        .trustManager(trustCertFile)
                        .build());
                } else {
                    nettyBuilder.useTransportSecurity();
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to configure TLS", e);
            }
            
            builder = nettyBuilder;
        } else {
            // if TLS is not enabled, use ManagedChannelBuilder
            builder = ManagedChannelBuilder.forTarget(properties.getServerAddress())
                .usePlaintext();
        }
        
        // Configure the channel with properties
        builder.keepAliveTime(properties.getKeepAliveTime(), TimeUnit.MILLISECONDS)
            .keepAliveTimeout(properties.getKeepAliveTimeout(), TimeUnit.MILLISECONDS)
            .keepAliveWithoutCalls(properties.isKeepAliveWithoutCalls())
            .idleTimeout(properties.getIdleTimeout(), TimeUnit.MILLISECONDS)
            .maxInboundMessageSize(properties.getMaxInboundMessageSize())
            .maxInboundMetadataSize(properties.getMaxInboundMetadataSize());
        
        // Configure retry policy if enabled
        if (properties.isEnableRetry()) {
            builder.enableRetry()
                .maxRetryAttempts(properties.getMaxRetryAttempts());
        } else {
            builder.disableRetry();
        }
        
        return builder.build();
    }
    
    /**
     * default HeartbeatService
     */
    @Bean
    @ConditionalOnMissingBean
    public HeartbeatService heartbeatService() {
        return new DefaultHeartbeatService();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientInfoService clientInfoService() {
        return new DefaultClientInfoService();
    }


    /**
     * create GrpcTunnelClientService
     */
    @Bean(name = "grpcTunnelClientService", destroyMethod = "destroy")
    @ConditionalOnMissingBean
    public GrpcTunnelClientService grpcTunnelClientService(
            ManagedChannel grpcTunnelChannel,
            GrpcTunnelClientProperties properties,
            ClientIdGenerator clientIdGenerator,
            @Autowired(required = false) List<MessageHandler> messageHandlers,
            HeartbeatService heartbeatService, ClientInfoService clientInfoService) {
        
        return new GrpcTunnelClientService(
            grpcTunnelChannel,
            properties,
            clientIdGenerator,
            messageHandlers,
            heartbeatService,
            clientInfoService
        );
    }
    

    /**
     * Get resource file from classpath or filesystem
     */
    private File getResourceFile(String resourcePath) throws IOException {
        if (resourcePath.startsWith("classpath:")) {
            Resource resource = applicationContext.getResource(resourcePath);
            return resource.getFile();
        } else {
            return new File(resourcePath);
        }
    }
}

