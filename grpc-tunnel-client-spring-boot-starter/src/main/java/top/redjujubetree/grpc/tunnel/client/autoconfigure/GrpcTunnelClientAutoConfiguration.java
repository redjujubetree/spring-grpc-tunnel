package top.redjujubetree.grpc.tunnel.client.autoconfigure;

import io.grpc.Channel;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory;
import net.devh.boot.grpc.client.config.GrpcChannelsProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import top.redjujubetree.grpc.tunnel.client.GrpcTunnelClientService;
import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;
import top.redjujubetree.grpc.tunnel.client.config.TunnelsProperties;
import top.redjujubetree.grpc.tunnel.client.id.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.client.id.DefaultClientIdGenerator;
import top.redjujubetree.grpc.tunnel.client.inject.GrpcClientTunnelBeanPostProcessor;
import top.redjujubetree.grpc.tunnel.client.service.ClientInfoService;
import top.redjujubetree.grpc.tunnel.client.service.DefaultClientInfoService;
import top.redjujubetree.grpc.tunnel.client.service.DefaultHeartbeatService;
import top.redjujubetree.grpc.tunnel.client.service.HeartbeatService;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GRPC Tunnel Autoconfiguration for Client
 * Supports multiple tunnel clients based on each gRPC client configuration
 */
@Setter
@Slf4j
@Configuration
@EnableConfigurationProperties(TunnelsProperties.class)
@ConditionalOnClass(GrpcChannelFactory.class)
public class GrpcTunnelClientAutoConfiguration {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private GrpcChannelFactory grpcChannelFactory;
    
    @Autowired
    private TunnelsProperties tunnelsProperties;
    
    @Autowired
    private GrpcChannelsProperties grpcChannelsProperties;
    
    @Autowired(required = false)
    @Lazy
    private List<MessageHandler> messageHandlers;
    
    /**
     * Default ClientIdGenerator
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientIdGenerator clientIdGenerator() {
        return new DefaultClientIdGenerator();
    }
    
    /**
     * Default HeartbeatService
     */
    @Bean
    @ConditionalOnMissingBean
    public HeartbeatService heartbeatService() {
        return new DefaultHeartbeatService();
    }

    /**
     * Default ClientInfoService
     */
    @Bean
    @ConditionalOnMissingBean
    public ClientInfoService clientInfoService() {
        return new DefaultClientInfoService();
    }

    @Bean
    public GrpcClientTunnelBeanPostProcessor grpcClientTunnelBeanPostProcessor(ApplicationContext applicationContext, TunnelClientRegistrar tunnelClientRegistrar) {
        return new GrpcClientTunnelBeanPostProcessor(applicationContext);
    }

    /**
     * Bean to register all tunnel clients
     * This bean will be created early in the Spring lifecycle
     */
    @Bean
    public TunnelClientRegistrar tunnelClientRegistrar(
            ClientIdGenerator clientIdGenerator,
            HeartbeatService heartbeatService,
            ClientInfoService clientInfoService) {
        
        log.info("Initializing Tunnel Client Registrar");
        TunnelClientRegistrar registrar = new TunnelClientRegistrar();
        
        // Register all tunnel clients immediately
        registrar.registerAllTunnelClients(
            clientIdGenerator,
            heartbeatService,
            clientInfoService
        );
        
        return registrar;
    }
    
    /**
     * Utility bean to get tunnel services by client name
     * This depends on tunnelClientRegistrar to ensure tunnels are registered first
     */
    @Bean
    @DependsOn("tunnelClientRegistrar")
    public TunnelServiceRegistry tunnelServiceRegistry() {
        return new TunnelServiceRegistry();
    }
    
    /**
     * Inner class to handle tunnel client registration
     */
    public class TunnelClientRegistrar {
        
        private final Map<String, GrpcTunnelClientService> registeredServices = new HashMap<>();
        
        /**
         * Register all tunnel clients based on configuration
         */
        public void registerAllTunnelClients(
                ClientIdGenerator clientIdGenerator,
                HeartbeatService heartbeatService,
                ClientInfoService clientInfoService) {
            
            log.info("Starting tunnel client registration ... ");
            log.info("Available gRPC clients: {}", grpcChannelsProperties.getClient().keySet());
            
            GrpcTunnelClientService primaryService = null;
            String firstClientName = null;
            int enabledCount = 0;
            
            // Get the bean factory for registration
            ConfigurableListableBeanFactory beanFactory = 
                ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
            
            // Iterate through all configured gRPC clients
            for (String clientName : grpcChannelsProperties.getClient().keySet()) {
                // Check if tunnel is enabled for this client
                if (tunnelsProperties.isTunnelEnabled(clientName)) {
                    log.info("Tunnel enabled for client: {}", clientName);
                    
                    GrpcTunnelClientService service = createAndRegisterTunnelClient(
                        clientName,
                        clientIdGenerator,
                        heartbeatService,
                        clientInfoService,
                        beanFactory
                    );
                    
                    if (service != null) {
                        registeredServices.put(clientName, service);
                        enabledCount++;
                        if (primaryService == null) {
                            primaryService = service;
                            firstClientName = clientName;
                        }
                    }
                } else {
                    log.debug("Tunnel not enabled for client: {}", clientName);
                }
            }
            
            // Register primary bean for backward compatibility
            if (enabledCount == 1 && primaryService != null) {
                try {
                    beanFactory.registerSingleton("grpcTunnelClientService", primaryService);
                    log.info("✓ Registered primary tunnel client service bean: grpcTunnelClientService");
                    log.info("✓ Tunnel service also available as bean: {}-tunnel", firstClientName);
                } catch (Exception e) {
                    log.warn("Failed to register primary tunnel service bean", e);
                }
            } else if (enabledCount > 1) {
                log.info("Multiple tunnel clients enabled ({}). Available beans:", enabledCount);
                registeredServices.forEach((name, service) -> {
                    log.info("  - {}-tunnel", name);
                });
                log.info("Use @Qualifier or @Resource with specific bean name to inject services.");
            } else if (enabledCount == 0) {
                log.warn("No tunnel clients are enabled. Check your configuration.");
                log.warn("Ensure grpc.client.{client-name}.tunnel.enabled=true for at least one client.");
            }
            
            log.info("Tunnel client registration completed. Registered {} services.", enabledCount);
        }
        
        /**
         * Create and register a single tunnel client service
         */
        private GrpcTunnelClientService createAndRegisterTunnelClient(
                String clientName,
                ClientIdGenerator clientIdGenerator,
                HeartbeatService heartbeatService,
                ClientInfoService clientInfoService,
                ConfigurableListableBeanFactory beanFactory) {
            
            try {
                log.debug("Creating tunnel client for: {}", clientName);
                // Register the service as a bean with name: {clientName}-tunnel
                String beanName = clientName + "-tunnel";

                // Get or create the channel for this client
                Channel channel = grpcChannelFactory.createChannel(clientName);


                if (channel == null) {
                    log.error("Failed to create channel for client: {}", clientName);
                    return null;
                }
                
                log.debug("Successfully created/retrieved channel for client: {}", clientName);
                
                // Get tunnel configuration for this client
                TunnelProperties tunnelProperties = tunnelsProperties.getTunnelConfig(clientName);
                String clientId = clientIdGenerator.generate(beanName, tunnelProperties);
                // Create the tunnel client service
                GrpcTunnelClientService tunnelService = new GrpcTunnelClientService(
                    (io.grpc.ManagedChannel) channel,
                    tunnelProperties,
                    clientId,
                    messageHandlers,
                    heartbeatService,
                    clientInfoService
                );
                
                // Initialize the service
                try {
                    tunnelService.afterPropertiesSet();
                    log.debug("Successfully initialized tunnel service for client: {}", clientName);
                } catch (Exception e) {
                    log.error("Failed to initialize tunnel service for client: {}", clientName, e);
                    return null;
                }
                

                
                // Initialize and autowire the bean
                applicationContext.getAutowireCapableBeanFactory()
                    .initializeBean(tunnelService, beanName);
                applicationContext.getAutowireCapableBeanFactory()
                    .autowireBean(tunnelService);
                
                // Register as singleton
                beanFactory.registerSingleton(beanName, tunnelService);
                
                log.info("✓ Registered tunnel client service bean: '{}' for gRPC client: '{}'", 
                        beanName, clientName);
                
                return tunnelService;
                
            } catch (Exception e) {
                log.error("Failed to create tunnel client for: {}", clientName, e);
                return null;
            }
        }
        
        /**
         * Get all registered services
         */
        public Map<String, GrpcTunnelClientService> getRegisteredServices() {
            return new HashMap<>(registeredServices);
        }
    }

    /**
     * Registry class to help retrieve tunnel services
     */
    public class TunnelServiceRegistry {
        
        /**
         * Get tunnel service by client name
         */
        public GrpcTunnelClientService getTunnelService(String clientName) {
            String beanName = clientName + "-tunnel";
            try {
                return applicationContext.getBean(beanName, GrpcTunnelClientService.class);
            } catch (Exception e) {
                log.debug("No tunnel service found for client: {}", clientName);
                return null;
            }
        }
        
        /**
         * Get all registered tunnel services
         */
        public Map<String, GrpcTunnelClientService> getAllTunnelServices() {
            Map<String, GrpcTunnelClientService> services = new HashMap<>();
            
            for (String clientName : grpcChannelsProperties.getClient().keySet()) {
                GrpcTunnelClientService service = getTunnelService(clientName);
                if (service != null) {
                    services.put(clientName, service);
                }
            }
            
            return services;
        }
        
        /**
         * Check if tunnel is enabled for a client
         */
        public boolean isTunnelEnabled(String clientName) {
            return getTunnelService(clientName) != null;
        }
        
        /**
         * Get the primary tunnel service (if only one exists)
         */
        public GrpcTunnelClientService getPrimaryTunnelService() {
            try {
                return applicationContext.getBean("grpcTunnelClientService", GrpcTunnelClientService.class);
            } catch (Exception e) {
                // Try to get the first available tunnel
                Map<String, GrpcTunnelClientService> all = getAllTunnelServices();
                if (all.size() == 1) {
                    return all.values().iterator().next();
                }
                return null;
            }
        }
    }
}