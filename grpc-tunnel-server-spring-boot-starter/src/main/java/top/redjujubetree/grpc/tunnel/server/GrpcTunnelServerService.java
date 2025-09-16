package top.redjujubetree.grpc.tunnel.server;

import com.google.protobuf.ByteString;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.Getter;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.GrpcTunnelServiceGrpc;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.config.GrpcTunnelServerProperties;
import top.redjujubetree.grpc.tunnel.server.connection.ClientConnection;
import top.redjujubetree.grpc.tunnel.server.connection.ConnectionManager;
import top.redjujubetree.grpc.tunnel.server.filter.ClientRegisterFilter;
import top.redjujubetree.grpc.tunnel.server.handler.ConnectionResult;
import top.redjujubetree.grpc.tunnel.server.handler.HeartbeatHandler;
import top.redjujubetree.grpc.tunnel.utils.JsonUtil;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Client Management is removed from the GrpcTunnelServerService, and now it is handled by ConnectionManager.
 * GRPC Tunnel Server Service implementation.
 * This service handles client connections, message processing, and heartbeat management.
 */
@GrpcService
public class GrpcTunnelServerService extends GrpcTunnelServiceGrpc.GrpcTunnelServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(GrpcTunnelServerService.class);
    
    private final ConnectionManager connectionManager;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> heartbeatCheckTask;
    
    private final GrpcTunnelServerProperties properties;
    private final List<ClientRegisterFilter> clientRegisterFilters;
    private final List<MessageHandler> messageHandlers;
    private final HeartbeatHandler heartbeatHandler;
    
    public GrpcTunnelServerService(
            GrpcTunnelServerProperties properties,
            List<ClientRegisterFilter> clientRegisterFilters,
            ConnectionManager connectionManager,
            List<MessageHandler> messageHandlers,
            HeartbeatHandler heartbeatHandler) {
        this.properties = properties;
        this.clientRegisterFilters = clientRegisterFilters != null ? clientRegisterFilters : Collections.emptyList();
        this.connectionManager = connectionManager;
        this.messageHandlers = messageHandlers != null ? messageHandlers : Collections.emptyList();
        this.heartbeatHandler = heartbeatHandler;
    }

    @PostConstruct
    public void init() {
        log.info("GRPC Tunnel Server starting with properties: {}", properties);

        clientRegisterFilters.sort(Comparator.comparingInt(ClientRegisterFilter::getOrder));

        messageHandlers.sort(Comparator.comparingInt(MessageHandler::getOrder));

        startHeartbeatChecker();

        log.info("GRPC Tunnel Server started successfully");
    }
    
    @PreDestroy
    public void destroy() {
        // stop heartbeat checker
        if (heartbeatCheckTask != null) {
            heartbeatCheckTask.cancel(true);
        }
        // close all client connections
        connectionManager.shutdown();

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("GRPC Tunnel Server shutdown completed");
    }
    
    /**
     * start heartbeat checker
     */
    private void startHeartbeatChecker() {
        long checkInterval = Math.min(properties.getHeartbeatTimeout() / 3, 10*1000);
        
        heartbeatCheckTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                checkClientHeartbeats();
            } catch (Exception e) {
                log.error("Error during heartbeat check", e);
            }
        }, checkInterval, checkInterval, TimeUnit.MILLISECONDS);
        
        log.info("Heartbeat checker started with interval: {}ms", checkInterval);
    }

    /**
     * check client heartbeats, if a client is inactive for too long, remove it.
     */
    private void checkClientHeartbeats() {
        List<String> inactiveClients = connectionManager.getInactiveClients(properties.getHeartbeatTimeout());
        for (String clientId : inactiveClients) {
            log.warn("Client {} heartbeat timeout, so removing it", clientId);
            if (heartbeatHandler != null) {
                try {
                    heartbeatHandler.handleTimeout(clientId);
                } catch (Exception e) {
                    log.error("Error in heartbeat handler for client: {}", clientId, e);
                }
            }
            connectionManager.removeClient(clientId, "Heartbeat timeout");
        }
    }

    @Override
    public StreamObserver<TunnelMessage> establishTunnel(StreamObserver<TunnelMessage> responseObserver) {
        return new StreamObserver<TunnelMessage>() {
            private volatile String clientId;
            private volatile ClientConnection connection;
            private volatile boolean isActive = true;

            @Override
            public void onNext(TunnelMessage message) {
                if (!isActive) {
                    log.warn("Received message for inactive connection: {}", clientId);
                    return;
                }

                try {
                    // init the client connection if not already done
                    if (clientId == null) {
                        if (!initializeConnection(message, responseObserver)) {
                            return;
                        }
                    }

                    // 更新活动时间和计数
                    connectionManager.recordMessageReceived(clientId);

                    // 处理消息
                    processMessage(message, responseObserver);

                } catch (Exception e) {
                    log.error("Error processing message from client: {}", clientId, e);
                    sendErrorResponse(responseObserver, message, 500, "Internal server error");
                }
            }

            private boolean initializeConnection(TunnelMessage message,
                                                 StreamObserver<TunnelMessage> responseObserver) {
                clientId = message.getClientId();

                // check if the client is already connected
                if (connectionManager.getActiveConnectionCount() >= properties.getMaxClients()) {
                    log.error("Max clients limit reached: {}", properties.getMaxClients());
                    sendErrorResponse(responseObserver, message, 503, "Server at capacity");
                    closeConnectionOnEstablishTunnelFailed(responseObserver);
                    return false;
                }

                RegisterRequest registerRequest = JsonUtil.fromJson(message.getRequest().getData().toStringUtf8(), RegisterRequest.class);
                Map<String, Object> metadata = new HashMap<>();
                for (ClientRegisterFilter clientRegisterFilter : clientRegisterFilters) {
                    ConnectionResult connectionResult = clientRegisterFilter.doFilter(message, registerRequest);
                    if (!connectionResult.isAccepted()) {
                        sendErrorResponse(responseObserver, message, 400, connectionResult.getMessage());
                        closeConnectionOnEstablishTunnelFailed(responseObserver);
                        return false;
                    }
                    if (connectionResult.getMetadata() != null && !connectionResult.getMetadata().isEmpty()) {
                        metadata.putAll(connectionResult.getMetadata());
                    }
                }
                if (connectionManager.hasClient(clientId)) {
                    log.error("Client ID already connected: {}", clientId);
                    sendErrorResponse(responseObserver, message, 409, "Client ID already connected");
                    closeConnectionOnEstablishTunnelFailed(responseObserver);
                    return false;
                }
                // create a new connection
                connection = new ClientConnection(clientId, responseObserver);
                connectionManager.addClient(connection);
                if (!metadata.isEmpty()) {
                    connection.setMetadata(metadata);
                }
                // send connection acknowledgment
                sendConnectionAck(responseObserver, message);

                log.info("Client {} connected successfully", clientId);
                return true;
            }

            private void processMessage(TunnelMessage message,
                                        StreamObserver<TunnelMessage> responseObserver) {
                // to handle heartbeat messages
                if (heartbeatHandler != null && heartbeatHandler.support(message)) {
                    heartbeatHandler.handleHeartbeat(message);
                    sendHeartbeatResponse(responseObserver, message);
                    return;
                }

                // to handle business messages
                handleBusinessMessage(message, responseObserver);
            }

            @Override
            public void onError(Throwable t) {
                if (!isActive) {
                    return;
                }

                isActive = false;
                if (t instanceof StatusRuntimeException) {
                    StatusRuntimeException sre = (StatusRuntimeException) t;
                    if (sre.getStatus().getCode() == Status.Code.CANCELLED) {
                        log.info("Client {} disconnected (cancelled by client)", clientId);
                    } else {
                        log.warn("Connection error for client: {} - {}", clientId, sre.getStatus());
                    }
                } else {
                    log.error("Unexpected connection error for client: {}", clientId, t);
                }


                if (clientId != null) {
                    connectionManager.removeClient(clientId, "Connection error: " + t.getMessage());
                }
            }

            @Override
            public void onCompleted() {
                if (!isActive) {
                    return;
                }

                isActive = false;
                log.info("Client disconnected normally: {}", clientId);

                if (clientId != null) {
                    connectionManager.removeClient(clientId, "Client disconnected normally");
                }
            }

            private void closeConnectionOnEstablishTunnelFailed(StreamObserver<TunnelMessage> observer) {
                try {
                    observer.onCompleted();
                } catch (Exception e) {
                    log.debug("Error closing response observer", e);
                }
            }
        };
    }

    private void handleBusinessMessage(TunnelMessage message, StreamObserver<TunnelMessage> responseObserver) {
        if (messageHandlers == null || messageHandlers.isEmpty()) {
            if (message.hasRequest()){
                log.warn("No message handlers configured for request {}", message.getRequest());
            }
            if (message.hasResponse()) {
                log.warn("No message handlers configured for response {}", message.getResponse());
            }
            sendErrorResponse(responseObserver, message, 501, "No handler available");
            return;
        }

        boolean handled = false;
        for (MessageHandler handler : messageHandlers) {
            if (handler.support(message)) {
                handled = true;
                handler.handle(message).whenComplete((response, error) -> {
                    if (error != null) {
                        log.error("Handler error for message: {}", message.getMessageId(), error);
                        sendErrorResponse(responseObserver, message, 500, error.getMessage());
                    } else if (response != null) {
                        ClientConnection conn = connectionManager.getClient(message.getClientId());
                        if (conn != null) {
                            conn.incrementSent();
                        }
                        responseObserver.onNext(response);
                    }
                });
                break;
            }
        }
        
        if (!handled) {
            log.warn("No handler found for message: {}, type: {}, data: {}", message.getMessageId(), message.getRequest().getType(),message.getRequest().getData().toStringUtf8());
        }
    }

    private void sendConnectionAck(StreamObserver<TunnelMessage> observer, TunnelMessage request) {
        TunnelMessage response = TunnelMessage.newBuilder()
            .setMessageId(UUID.randomUUID().toString())
            .setClientId(request.getClientId())
            .setType(MessageType.SERVER_RESPONSE)
            .setTimestamp(System.currentTimeMillis())
            .setCorrelationId(request.getMessageId())
            .setResponse(ResponsePayload.newBuilder()
                .setCode(200)
                .setMessage("CONNECTION_ACK")
                .setData(ByteString.copyFromUtf8("{\"status\":\"connected\"}"))
                .build())
            .build();
        observer.onNext(response);
    }
    
    private void sendHeartbeatResponse(StreamObserver<TunnelMessage> observer, TunnelMessage request) {
        TunnelMessage response = TunnelMessage.newBuilder()
            .setMessageId(UUID.randomUUID().toString())
            .setClientId(request.getClientId())
            .setType(MessageType.SERVER_RESPONSE)
            .setTimestamp(System.currentTimeMillis())
            .setCorrelationId(request.getMessageId())
            .setResponse(ResponsePayload.newBuilder()
                .setCode(200)
                .setMessage("HEARTBEAT_ACK")
                .build())
            .build();
        observer.onNext(response);
    }
    
    private void sendErrorResponse(StreamObserver<TunnelMessage> observer, TunnelMessage request, 
                                   int code, String message) {
        log.info("Sending error response for request: {}, code: {}, message: {}",
                request.getMessageId(), code, message);
        TunnelMessage response = TunnelMessage.newBuilder()
            .setMessageId(UUID.randomUUID().toString())
            .setClientId(request.getClientId())
            .setType(MessageType.SERVER_RESPONSE)
            .setTimestamp(System.currentTimeMillis())
            .setCorrelationId(request.getMessageId())
            .setResponse(ResponsePayload.newBuilder()
                .setCode(code)
                .setMessage(message)
                .build())
            .build();
        observer.onNext(response);
    }

    /**
     * get all connected clients
     */
    public Set<String> getConnectedClients() {
        return connectionManager.getAllClientIds();
    }

    public Optional<ClientConnectionInfo> getClientInfo(String clientId) {
        Optional<ClientConnection> optional = connectionManager.getConnection(clientId);
        if (optional.isPresent()) {
            ClientConnection connection = optional.get();
            return Optional.of(new ClientConnectionInfo(
                    connection.getClientId(),
                    connection.getConnectedAt(),
                    connection.getLastActivity(),
                    connection.getMessagesSent(),
                    connection.getMessagesReceived()
            ));
        }
        return Optional.empty();
    }

    @Getter
    public static class ClientConnectionInfo {
        private final String clientId;
        private final long connectedAt;
        private final long lastActivity;
        private final long messagesSent;
        private final long messagesReceived;
        
        public ClientConnectionInfo(String clientId, long connectedAt, long lastActivity, 
                                   long messagesSent, long messagesReceived) {
            this.clientId = clientId;
            this.connectedAt = connectedAt;
            this.lastActivity = lastActivity;
            this.messagesSent = messagesSent;
            this.messagesReceived = messagesReceived;
        }
    }
}