package top.redjujubetree.grpc.tunnel.server;

import com.alibaba.fastjson.JSON;
import com.google.protobuf.ByteString;
import lombok.Getter;
import top.redjujubetree.grpc.tunnel.handler.HeartbeatHandler;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.proto.GrpcTunnelServiceGrpc;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.config.GrpcTunnelServerProperties;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@GrpcService
public class GrpcTunnelServerService extends GrpcTunnelServiceGrpc.GrpcTunnelServiceImplBase {
    
    private static final Logger log = LoggerFactory.getLogger(GrpcTunnelServerService.class);
    
    private final Map<String, ClientConnection> clients = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> heartbeatCheckTask;
    
    private final GrpcTunnelServerProperties properties;
    private final List<MessageHandler> messageHandlers;
    private final HeartbeatHandler heartbeatHandler;
    
    public GrpcTunnelServerService(
            GrpcTunnelServerProperties properties,
            List<MessageHandler> messageHandlers,
            HeartbeatHandler heartbeatHandler) {
        this.properties = properties;
        this.messageHandlers = messageHandlers != null ? messageHandlers : Collections.emptyList();
        this.heartbeatHandler = heartbeatHandler;
        
        startHeartbeatChecker();
        log.info("GRPC Tunnel Server initialized with heartbeat timeout: {}ms", 
            properties.getHeartbeatTimeout());
    }
    
    // Default constructor to maintain backward compatibility
    public GrpcTunnelServerService() {
        // Use default values or delegate to configuration
        this(
            // You might want to create a method to get default properties
            new GrpcTunnelServerProperties(), 
            Collections.emptyList(), 
            null
        );
    }
    
    @PreDestroy
    public void destroy() {
        // stop heartbeat checker
        if (heartbeatCheckTask != null) {
            heartbeatCheckTask.cancel(true);
        }
        
        // close all client connections
        clients.values().forEach(connection -> {
            try {
                connection.getObserver().onCompleted();
            } catch (Exception e) {
                log.error("Error closing connection for client: {}", connection.getClientId(), e);
            }
        });
        clients.clear();
        
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        log.info("GRPC Tunnel Server destroyed");
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
        long currentTime = System.currentTimeMillis();
        long timeout = properties.getHeartbeatTimeout();
        
        Iterator<Map.Entry<String, ClientConnection>> iterator = clients.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ClientConnection> entry = iterator.next();
            String clientId = entry.getKey();
            ClientConnection connection = entry.getValue();
            
            long inactiveTime = currentTime - connection.getLastActivity();
            
            if (inactiveTime > timeout) {
                log.warn("Client {} heartbeat timeout, inactive for {}ms", clientId, inactiveTime);
                
                if (heartbeatHandler != null) {
                    try {
                        heartbeatHandler.handleTimeout(clientId);
                    } catch (Exception e) {
                        log.error("Error in heartbeat handler for client: {}", clientId, e);
                    }
                }
                
                try {
                    connection.getObserver().onCompleted();
                } catch (Exception e) {
                    log.error("Error closing connection for client: {}", clientId, e);
                }
                
                // remove client
                iterator.remove();
                
                log.info("Client {} removed due to heartbeat timeout", clientId);
            }
        }
    }
    
    @Override
    public StreamObserver<TunnelMessage> establishTunnel(StreamObserver<TunnelMessage> responseObserver) {
        return new StreamObserver<TunnelMessage>() {
            private String clientId;
            private ClientConnection connection;
            
            @Override
            public void onNext(TunnelMessage message) {
                try {
                    if (clientId == null) {
                        clientId = message.getClientId();
                        
                        if (clients.size() >= properties.getMaxClients()) {
                            log.error("Max clients limit reached: {}", properties.getMaxClients());
                            sendErrorResponse(responseObserver, message, 503, "Server at capacity");
                            responseObserver.onCompleted();
                            return;
                        }
                        
                        ClientConnection existingConnection = clients.get(clientId);
                        if (existingConnection != null) {
                            log.warn("Client {} already connected, closing old connection", clientId);
                            removeClient(clientId);
                        }
                        
                        connection = new ClientConnection(clientId, responseObserver);
                        clients.put(clientId, connection);
                        log.info("Client connected: {} (Total clients: {})", clientId, clients.size());

                        sendConnectionAck(responseObserver, message);
                    }
                    
                    connection.updateLastActivity();
                    
                    connection.incrementReceived();

                    if (heartbeatHandler.supports(message)) {
                        if (heartbeatHandler != null) {
                            heartbeatHandler.handleHeartbeat(message);
                        } else {
                            log.warn("No heartbeat handler configured, ignoring heartbeat message for client: {}, msg: {}", clientId, JSON.toJSONString(message));
                        }
                        sendHeartbeatResponse(responseObserver, message);
                        return;
                    }
                    // do handling of business messages
                    handleBusinessMessage(message, responseObserver);


                } catch (Exception e) {
                    log.error("Error processing message from client: {}", clientId, e);
                    sendErrorResponse(responseObserver, message, 500, "Internal server error");
                }
            }
            
            @Override
            public void onError(Throwable t) {
                log.error("Connection error for client: {}", clientId, t);
                removeClient(clientId);
            }
            
            @Override
            public void onCompleted() {
                log.info("Client disconnected normally: {}", clientId);
                removeClient(clientId);
                responseObserver.onCompleted();
            }
        };
    }
    
    private void handleBusinessMessage(TunnelMessage message, StreamObserver<TunnelMessage> responseObserver) {
        if (messageHandlers == null || messageHandlers.isEmpty()) {
            log.warn("No message handlers configured for message type: {}", message.getRequest().getType());
            sendErrorResponse(responseObserver, message, 501, "No handler available");
            return;
        }
        
        messageHandlers.sort(Comparator.comparingInt(MessageHandler::getOrder));
        
        boolean handled = false;
        for (MessageHandler handler : messageHandlers) {
            if (handler.supports(message)) {
                handled = true;
                handler.handle(message).whenComplete((response, error) -> {
                    if (error != null) {
                        log.error("Handler error for message: {}", message.getMessageId(), error);
                        sendErrorResponse(responseObserver, message, 500, error.getMessage());
                    } else if (response != null) {
                        ClientConnection conn = clients.get(message.getClientId());
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
            sendErrorResponse(responseObserver, message, 404, "No handler found for type: " + message.getRequest().getType());
        }
    }
    
    private void removeClient(String clientId) {
        if (clientId == null) {
            return;
        }
        
        ClientConnection connection = clients.remove(clientId);
        if (connection != null) {
            log.info("Client removed: {} (Total clients: {})", clientId, clients.size());
            try {
                connection.getObserver().onCompleted();
            } catch (Exception e) {
                // 连接可能已经关闭
                log.debug("Error closing connection for client: {}", clientId, e);
            }
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
     * send a message to a specific client
     */
    public boolean sendToClient(String clientId, TunnelMessage message) {
        ClientConnection connection = clients.get(clientId);
        if (connection != null) {
            try {
                connection.incrementSent();
                connection.getObserver().onNext(message);
                return true;
            } catch (Exception e) {
                log.error("Error sending message to client: {}", clientId, e);
                removeClient(clientId);
                return false;
            }
        } else {
            log.warn("Client {} not found", clientId);
            return false;
        }
    }
    
    /**
     * send a message to all connected clients
     */
    public void broadcast(TunnelMessage message) {
        List<String> failedClients = new ArrayList<>();
        
        clients.forEach((clientId, connection) -> {
            try {
                connection.incrementSent();
                connection.getObserver().onNext(message);
            } catch (Exception e) {
                log.error("Error broadcasting to client: {}", clientId, e);
                failedClients.add(clientId);
            }
        });
        
        failedClients.forEach(this::removeClient);
    }
    
    /**
     * get all connected clients
     */
    public Set<String> getConnectedClients() {
        return new HashSet<>(clients.keySet());
    }
    
    public Optional<ClientConnectionInfo> getClientInfo(String clientId) {
        ClientConnection connection = clients.get(clientId);
        if (connection != null) {
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
    
    /**
     * Inner class to hold client connection information
     */
    private static class ClientConnection {
        private final String clientId;
        private final StreamObserver<TunnelMessage> observer;
        private final long connectedAt;
        private volatile long lastActivity;
        private final AtomicLong messagesSent;
        private final AtomicLong messagesReceived;
        
        public ClientConnection(String clientId, StreamObserver<TunnelMessage> observer) {
            this.clientId = clientId;
            this.observer = observer;
            this.connectedAt = System.currentTimeMillis();
            this.lastActivity = System.currentTimeMillis();
            this.messagesSent = new AtomicLong(0);
            this.messagesReceived = new AtomicLong(0);
        }
        
        public void updateLastActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void incrementSent() {
            messagesSent.incrementAndGet();
        }
        
        public void incrementReceived() {
            messagesReceived.incrementAndGet();
        }
        
        // getters
        public String getClientId() { return clientId; }
        public StreamObserver<TunnelMessage> getObserver() { return observer; }
        public long getConnectedAt() { return connectedAt; }
        public long getLastActivity() { return lastActivity; }
        public long getMessagesSent() { return messagesSent.get(); }
        public long getMessagesReceived() { return messagesReceived.get(); }
    }
    
    /**
     * Statistic info for a client connection
     */
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