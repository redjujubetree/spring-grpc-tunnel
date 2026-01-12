package top.redjujubetree.grpc.tunnel.client;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;
import top.redjujubetree.grpc.tunnel.client.service.ClientInfoService;
import top.redjujubetree.grpc.tunnel.client.service.DefaultHeartbeatService;
import top.redjujubetree.grpc.tunnel.client.service.HeartbeatService;
import top.redjujubetree.grpc.tunnel.constant.ClientRequestTypes;
import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.*;
import top.redjujubetree.grpc.tunnel.utils.TunnelMessagesUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * GRPC Tunnel Client Service - Enhanced connection detection and reconnection mechanism
 */
public class GrpcTunnelClientService implements InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(GrpcTunnelClientService.class);
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    // Injected dependencies
    private ManagedChannel channel;
    private TunnelProperties properties;
    private List<MessageHandler> messageHandlers;
    private HeartbeatService heartbeatService;
    private ClientInfoService clientInfoService;

    // gRPC related
    private GrpcTunnelServiceGrpc.GrpcTunnelServiceStub tunnelStub;
    private String clientId;
    private StreamObserver<TunnelMessage> requestObserver;

    // Connection state management
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean isShuttingDown = new AtomicBoolean(false);
    private int reconnectAttempts = 0;

    // Health detection related
    private final AtomicLong lastHeartbeatTime = new AtomicLong(0);
    private final AtomicLong lastServerResponseTime = new AtomicLong(0);
    private int consecutiveHeartbeatFailures = 0;

    // Async task management
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, CompletableFuture<TunnelMessage>> pendingRequests = new ConcurrentHashMap<>();
    private ScheduledFuture<?> heartbeatTask;
    private ScheduledFuture<?> reconnectTask;

    private final BlockingQueue<TunnelMessage> sendQueue = new LinkedBlockingQueue<>();
    private final ExecutorService sendExecutor =
            Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "grpc-send-thread");
                t.setDaemon(true);
                return t;
            });

    public GrpcTunnelClientService(){}
    /**
     * Constructor to inject all necessary dependencies
     */
    public GrpcTunnelClientService(ManagedChannel channel,
                                   TunnelProperties properties,
                                   String clientId,
                                   List<MessageHandler> messageHandlers,
                                   HeartbeatService heartbeatService,
                                   ClientInfoService clientInfoService) {
        this.channel = channel;
        this.properties = properties;
        this.clientId = clientId;
        this.messageHandlers = messageHandlers != null ? messageHandlers : Collections.emptyList();
        this.heartbeatService = heartbeatService != null ? heartbeatService : new DefaultHeartbeatService();
        this.clientInfoService = clientInfoService;
    }

    public Channel getChannel() {
        return channel;
    }

    @Override
    public void afterPropertiesSet() {
        this.tunnelStub = GrpcTunnelServiceGrpc.newStub(channel);

        log.info("GRPC Tunnel Client initialized with ID: {}", clientId);

        if (properties.isAutoConnect()) {
            connect();
        }
    }

    @Override
    public void destroy() {
        isShuttingDown.set(true);

        disconnect();
        cancelAllTasks();
        shutdownScheduler();
        completePendingRequests();

        log.info("GRPC Tunnel Client destroyed");
    }

    private void startSendLoop() {
        sendExecutor.execute(() -> {
            while (!isShuttingDown.get()) {
                try {
                    TunnelMessage msg = sendQueue.take();
                    if (requestObserver != null && connected.get()) {
                        requestObserver.onNext(msg);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Send loop error", e);
                }
            }
        });
    }

    private void enqueueSend(TunnelMessage message) {
        if (!connected.get()) {
            log.warn("Drop message, not connected");
            return;
        }
        sendQueue.offer(message);
    }

    // ==================== Connection Management ====================

    /**
     * Connect to gRPC server
     */
    public synchronized void connect() {
        if (connected.get()) {
            log.info("Already connected to server");
            return;
        }

        if (isShuttingDown.get()) {
            log.info("Client is shutting down, skip connection");
            return;
        }

        try {
            resetConnectionState();

            log.info("Attempting to establish tunnel connection...");
            StreamObserver<TunnelMessage> responseObserver = createResponseObserver();
            requestObserver = tunnelStub.establishTunnel(responseObserver);

            // Set connected to true temporarily to allow sendRequest to work
            connected.set(true);
            // start send thread
            startSendLoop();

            if (sendConnectionMessage()) {
                reconnectAttempts = 0;
                consecutiveHeartbeatFailures = 0;
                lastServerResponseTime.set(System.currentTimeMillis());

                startHeartbeat();

                log.info("{} Connected to server successfully", getClientId());
            } else {
                log.warn("Connection validation failed");
                connected.set(false);
                cleanupFailedConnection();
                throw new RuntimeException("Connection validation timeout or failed");
            }
        } catch (Exception e) {
            log.error("Failed to connect to server", e);
            connected.set(false);
            cleanupFailedConnection();

            if (!isShuttingDown.get()) {
                scheduleReconnect();
            }
        }
    }

    /**
     * Disconnect from gRPC server
     */
    public synchronized void disconnect() {
        if (!connected.get()) {
            return;
        }

        connected.set(false);
        stopHeartbeat();

        try {
            if (requestObserver != null) {
                sendDisconnectionMessage();
                Thread.sleep(100);
                requestObserver.onCompleted();
            }
        } catch (Exception e) {
            log.debug("Error during disconnect (expected if server already down)", e);
        }

        log.info("Disconnected from server");
    }

    /**
     * Force reconnection due to connection issues
     */
    private void forceReconnect(String reason) {
        log.info("Force reconnecting due to: {}", reason);

        connected.set(false);
        stopHeartbeat();

        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception e) {
                log.debug("Error closing request observer: {}", e.getMessage());
            }
            requestObserver = null;
        }

        if (!isShuttingDown.get() && properties.isAutoReconnect()) {
            scheduleReconnect();
        }
    }

    /**
     * Reset connection state variables
     */
    private void resetConnectionState() {
        consecutiveHeartbeatFailures = 0;
        lastHeartbeatTime.set(0);
        lastServerResponseTime.set(0);
    }

    /**
     * Clean up failed connection
     */
    private void cleanupFailedConnection() {
        if (requestObserver != null) {
            try {
                requestObserver.onCompleted();
            } catch (Exception e) {
                log.debug("Error closing failed connection observer", e);
            }
            requestObserver = null;
        }
    }

    // ==================== Heartbeat and Health Check ====================

    /**
     * Start heartbeat task with integrated connection health check
     */
    private void startHeartbeat() {
        stopHeartbeat();

        long heartbeatInterval = properties.getHeartbeatInterval();

        heartbeatTask = scheduler.scheduleWithFixedDelay(() -> {
            if (!connected.get() || isShuttingDown.get()) {
                return;
            }

            // Check connection health status
            if (isConnectionUnhealthy()) {
                return; // Connection is unhealthy, will trigger reconnection
            }

            // Send heartbeat
            sendHeartbeat();

        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);

        log.debug("Heartbeat task started with interval: {}ms", heartbeatInterval);
    }

    /**
     * Stop heartbeat task
     */
    private void stopHeartbeat() {
        if (heartbeatTask != null && !heartbeatTask.isDone()) {
            heartbeatTask.cancel(false);
            heartbeatTask = null;
            log.debug("Heartbeat task stopped");
        }
    }

    /**
     * Check if connection is unhealthy
     */
    private boolean isConnectionUnhealthy() {
        long now = System.currentTimeMillis();
        long lastResponse = lastServerResponseTime.get();
        long heartbeatInterval = properties.getHeartbeatInterval();

        // Check response timeout (no response for more than 3 heartbeat intervals)
        if (lastResponse > 0 && (now - lastResponse) > (heartbeatInterval * 3)) {
            log.warn("Connection unhealthy: no server response for {}ms (last response: {})",
                    (now - lastResponse), new Date(lastResponse));
            forceReconnect("No server response timeout");
            return true;
        }

        // Check consecutive heartbeat failures
        if (consecutiveHeartbeatFailures >= MAX_CONSECUTIVE_FAILURES) {
            log.warn("Connection unhealthy: {} consecutive heartbeat failures", consecutiveHeartbeatFailures);
            forceReconnect("Too many consecutive heartbeat failures");
            return true;
        }

        return false;
    }

    /**
     * Send heartbeat message to server
     */
    private void sendHeartbeat() {
        try {
            lastHeartbeatTime.set(System.currentTimeMillis());

            Object heartbeatInfo = heartbeatService.generateHeartbeat(this.getClientId());
            log.debug("Sending heartbeat: message={}", heartbeatInfo);

            CompletableFuture<TunnelMessage> future = sendRequest(ClientRequestTypes.HEARTBEAT, TunnelMessagesUtil.serializeObj(heartbeatInfo));
            future.whenComplete((response, error) -> {
                if (error != null) {
                    log.warn("Heartbeat send failed: {}", error.getMessage());
                    handleHeartbeatFailure();
                } else {
                    ResponsePayload resp = response.getResponse();
                    log.debug("Heartbeat response received:  code={}, message={}, data={}", resp.getCode(), resp.getMessage(), resp.getData().toStringUtf8());
                    lastServerResponseTime.set(System.currentTimeMillis());
                    consecutiveHeartbeatFailures = 0; // Reset failure count on success
                }
            });
        } catch (Exception e) {
            log.warn("Failed to send heartbeat: {}", e.getMessage());
            handleHeartbeatFailure();
        }
    }

    /**
     * Handle heartbeat sending failure
     */
    private void handleHeartbeatFailure() {
        consecutiveHeartbeatFailures++;
        log.warn("Heartbeat failure count increased to: {}/{}",
                consecutiveHeartbeatFailures, MAX_CONSECUTIVE_FAILURES);
    }

    // ==================== Message Processing ====================

    /**
     * Create response observer to handle server responses
     */
    private StreamObserver<TunnelMessage> createResponseObserver() {
        return new StreamObserver<TunnelMessage>() {
            @Override
            public void onNext(TunnelMessage message) {
                // Update last server response time
                lastServerResponseTime.set(System.currentTimeMillis());
                // Reset consecutive failure count
                consecutiveHeartbeatFailures = 0;

                handleServerMsg(message);
            }

            @Override
            public void onError(Throwable t) {
                log.error("Connection error: {}", t.getMessage(), t);

                if (isRecoverableError(t)) {
                    log.info("Recoverable error detected, will attempt to reconnect");
                } else {
                    log.error("Non-recoverable error", t);
                }

                connected.set(false);
                stopHeartbeat();

                if (!isShuttingDown.get() && properties.isAutoReconnect()) {
                    scheduleReconnect();
                }
            }

            @Override
            public void onCompleted() {
                log.info("Server closed connection gracefully");
                connected.set(false);
                stopHeartbeat();

                if (!isShuttingDown.get() && properties.isAutoReconnect()) {
                    scheduleReconnect();
                }
            }
        };
    }

    /**
     * Handle server response or command message
     */
    private void handleServerMsg(TunnelMessage message) {
        if (message == null) {
            log.warn("Received invalid message, message is null");
            return;
        }
        if (MessageType.SERVER_RESPONSE.equals(message.getType())) {
            log.debug("Received server response: type={}, messageId={}, correlationId={}, code={}, message={}, data={}",
                    message.getType(), message.getMessageId(), message.getCorrelationId(), message.getResponse().getCode(), message.getResponse().getMessage(), message.getResponse().getData());
        }
        if (MessageType.SERVER_REQUEST.equals(message.getType())) {
            log.debug("Received server request: type={}, messageId={}, correlationId={}, requestType={}, payload={}",
                    message.getType(), message.getMessageId(), message.getCorrelationId(),
                    message.getRequest().getType(), message.getRequest().getData().toStringUtf8());
        }


        // Handle request responses
		message.getCorrelationId();
		if (!message.getCorrelationId().isEmpty()) {
            CompletableFuture<TunnelMessage> future = pendingRequests.remove(message.getCorrelationId());
            if (future != null) {
                future.complete(message);
                return;
            }
        }
        // Handle server-pushed messages
        if (!messageHandlers.isEmpty()) {
            for (MessageHandler handler : messageHandlers) {
                if (handler.support(message)) {
                    handler.handle(message).whenComplete((response, error) -> {
                        if (error != null) {
                            log.error("Handler error", error);
                        } else if (response != null) {
                            enqueueSend(response);
                        }
                    });
                    break;
                }
            }
        }
    }

    /**
     * Check if error is recoverable
     */
    private boolean isRecoverableError(Throwable t) {
        if (t instanceof StatusRuntimeException) {
            Status status = ((StatusRuntimeException) t).getStatus();
            Status.Code code = status.getCode();

            return code == Status.Code.UNAVAILABLE ||
                    code == Status.Code.DEADLINE_EXCEEDED ||
                    code == Status.Code.ABORTED ||
                    code == Status.Code.INTERNAL ||
                    code == Status.Code.CANCELLED;
        }

        return true;
    }

    // ==================== Message Sending ====================

    /**
     * Send connection message to server with validation
     */
    private boolean sendConnectionMessage() {
        try {
            RegisterRequest obj = clientInfoService.buildClientInfoPayload(this);
            log.info("Sending connection message: {}", obj);
            String clientPayload = TunnelMessagesUtil.serializeObj(obj);
            CompletableFuture<TunnelMessage> future = sendRequest(ClientRequestTypes.CONNECT, clientPayload, 5000);
            TunnelMessage response = future.get(6, TimeUnit.SECONDS); // Wait for connection confirmation
            log.info("Connection response received: {}", response.getResponse().getData().toStringUtf8());
            boolean success = response.hasResponse() && response.getResponse().getCode() == 200;

            log.debug("Connection validation result: success={}", success);
            return success;

        } catch (ExecutionException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("Connection validation timed out. Message: {}", e.getCause().getMessage());
            } else {
                log.error("Connection validation failed", e.getCause());
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Send disconnection message to server
     */
    private void sendDisconnectionMessage() {
        TunnelMessage disconnectMsg = TunnelMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setClientId(clientId)
                .setType(MessageType.CLIENT_REQUEST)
                .setTimestamp(System.currentTimeMillis())
                .setRequest(RequestPayload.newBuilder()
                        .setType("DISCONNECT")
                        .setData(ByteString.copyFromUtf8("{}"))
                        .build())
                .build();
        requestObserver.onNext(disconnectMsg);
    }

    /**
     * Send request and wait for response with default timeout
     */
    public CompletableFuture<TunnelMessage> sendRequest(String type, String data) {
        return sendRequest(type, data, properties.getRequestTimeout());
    }

    /**
     * Send request and wait for response with custom timeout
     */
    public CompletableFuture<TunnelMessage> sendRequest(String type, String data, long timeoutMillis) {
        if (!connected.get()) {
            CompletableFuture<TunnelMessage> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Not connected to server"));
            return future;
        }

        String messageId = UUID.randomUUID().toString();
        TunnelMessage request = TunnelMessage.newBuilder()
                .setMessageId(messageId)
                .setClientId(clientId)
                .setType(MessageType.CLIENT_REQUEST)
                .setTimestamp(System.currentTimeMillis())
                .setRequest(RequestPayload.newBuilder()
                        .setType(type)
                        .setData(ByteString.copyFromUtf8(data))
                        .build())
                .build();

        CompletableFuture<TunnelMessage> future = new CompletableFuture<>();
        pendingRequests.put(messageId, future);

        // Set timeout task
        ScheduledFuture<?> timeoutTask = scheduler.schedule(() -> {
            CompletableFuture<TunnelMessage> pendingFuture = pendingRequests.remove(messageId);
            if (pendingFuture != null && !pendingFuture.isDone()) {
                pendingFuture.completeExceptionally(
                        new TimeoutException(String.format("Request timeout after %dms", timeoutMillis)));
            }
        }, timeoutMillis, TimeUnit.MILLISECONDS);

        // Ensure timeout task is cancelled when future completes
        future.whenComplete((result, error) -> timeoutTask.cancel(false));

        try {
            enqueueSend(request);
            log.debug("Request sent: type={}, messageId={}", type, messageId);
        } catch (Exception e) {
            pendingRequests.remove(messageId);
            future.completeExceptionally(e);
            log.error("Failed to send request", e);
        }

        return future;
    }

    /**
     * Send one-way message (no response expected)
     */
    public void sendOneWay(String type, String data) {
        if (!connected.get()) {
            log.warn("Cannot send message, not connected to server");
            return;
        }

        TunnelMessage message = TunnelMessage.newBuilder()
                .setMessageId(UUID.randomUUID().toString())
                .setClientId(clientId)
                .setType(MessageType.CLIENT_REQUEST)
                .setTimestamp(System.currentTimeMillis())
                .setRequest(RequestPayload.newBuilder()
                        .setType(type)
                        .setData(ByteString.copyFromUtf8(data))
                        .build())
                .build();

        enqueueSend(message);
        log.debug("One-way message sent: type={}", type);
    }

    // ==================== Reconnection Mechanism ====================

    /**
     * Schedule reconnection attempt - Fixed reconnect task checking logic
     */
    private void scheduleReconnect() {
        if (!properties.isAutoReconnect() || isShuttingDown.get()) {
            log.debug("Skip reconnect: autoReconnect={}, shuttingDown={}",
                    properties.isAutoReconnect(), isShuttingDown.get());
            return;
        }

        if (properties.getMaxReconnectAttempts() != -1 &&
                reconnectAttempts >= properties.getMaxReconnectAttempts()) {
            log.error("Max reconnect attempts reached: {}", properties.getMaxReconnectAttempts());
            return;
        }

        // Fixed: Check and cancel existing reconnect task properly
        synchronized (this) {
            if (reconnectTask != null) {
                if (!reconnectTask.isDone()) {
                    // If task is still pending or executing, cancel it
                    log.debug("Cancelling existing reconnect task to schedule new one");
                    reconnectTask.cancel(false);
                } else {
                    log.debug("Previous reconnect task is done, can schedule new one");
                }
            }

            reconnectAttempts++;
            long delay = calculateReconnectDelay();

            reconnectTask = scheduler.schedule(() -> {
                try {
                    if (!isShuttingDown.get()) {
                        log.info("Executing reconnect attempt {}/{}",
                                reconnectAttempts,
                                properties.getMaxReconnectAttempts() == -1 ? "∞" : properties.getMaxReconnectAttempts());
                        connect();
                    } else {
                        log.debug("Skip reconnect execution: client is shutting down");
                    }
                } catch (Exception e) {
                    log.error("Error during reconnect execution", e);
                } finally {
                    // Clear the task reference when completed
                    synchronized (GrpcTunnelClientService.this) {
                        if (reconnectTask != null && reconnectTask.isDone()) {
                            reconnectTask = null;
                        }
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);

            log.info("Reconnection scheduled in {}ms (attempt: {})", delay, reconnectAttempts);
        }
    }

    /**
     * Force immediate reconnection (useful for startup failures)
     */
    public void reconnectImmediately() {
        log.info("Immediate reconnection requested");

        synchronized (this) {
            // Cancel any existing reconnect task
            if (reconnectTask != null && !reconnectTask.isDone()) {
                reconnectTask.cancel(false);
            }

            // Schedule immediate reconnection
            reconnectTask = scheduler.schedule(() -> {
                try {
                    if (!isShuttingDown.get()) {
                        log.info("Executing immediate reconnection");
                        connect();
                    }
                } catch (Exception e) {
                    log.error("Error during immediate reconnection", e);
                }
            }, 0, TimeUnit.MILLISECONDS);

            log.info("Immediate reconnection scheduled");
        }
    }

    /**
     * Calculate reconnection delay with exponential backoff strategy
     */
    private long calculateReconnectDelay() {
        if (!properties.isExponentialBackoff()) {
            return properties.getReconnectDelay();
        }
        if (reconnectAttempts <= 0) {
            return properties.getReconnectDelay();
        }
        // Use quadratic backoff instead of exponential for gentler growth
        long delay = Math.max(properties.getReconnectDelay(), reconnectAttempts * reconnectAttempts * 1000L);
        return Math.min(delay, properties.getMaxReconnectDelay());
    }

    // ==================== Resource Management ====================

    /**
     * Cancel all scheduled tasks
     */
    private void cancelAllTasks() {
        stopHeartbeat();

        if (reconnectTask != null && !reconnectTask.isDone()) {
            reconnectTask.cancel(false);
            reconnectTask = null;
        }
    }

    /**
     * Shutdown the scheduler gracefully
     */
    private void shutdownScheduler() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Scheduler did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Complete all pending requests with shutdown exception
     */
    private void completePendingRequests() {
        pendingRequests.forEach((id, future) ->
                future.completeExceptionally(new IllegalStateException("Client shutting down")));
        pendingRequests.clear();
    }

    // ==================== Status Query Methods ====================

    /**
     * Check if client is connected to server
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Get client ID
     */
    public String getClientId() {
        return clientId;
    }

    public TunnelProperties getTunnelConfig() {
        return properties;
    }

    /**
     * Get current reconnection attempts count
     */
    public int getReconnectAttempts() {
        return reconnectAttempts;
    }

    /**
     * Get pending request count
     */
    public int getPendingRequestCount() {
        return pendingRequests.size();
    }

    /**
     * Get comprehensive connection health information
     */
    public Map<String, Object> getConnectionHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("connected", connected.get());
        health.put("lastHeartbeatTime", lastHeartbeatTime.get());
        health.put("lastServerResponseTime", lastServerResponseTime.get());
        health.put("consecutiveHeartbeatFailures", consecutiveHeartbeatFailures);
        health.put("reconnectAttempts", reconnectAttempts);
        health.put("pendingRequests", pendingRequests.size());

        long now = System.currentTimeMillis();
        long lastResponse = lastServerResponseTime.get();
        if (lastResponse > 0) {
            health.put("timeSinceLastResponse", now - lastResponse);
        }
        return health;
    }

    /**
     * Get reconnection task status (for debugging)
     */
    public Map<String, Object> getReconnectStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("reconnectAttempts", reconnectAttempts);
        status.put("maxReconnectAttempts", properties.getMaxReconnectAttempts());
        status.put("autoReconnect", properties.isAutoReconnect());
        status.put("hasReconnectTask", reconnectTask != null);

        if (reconnectTask != null) {
            status.put("taskIsDone", reconnectTask.isDone());
            status.put("taskIsCancelled", reconnectTask.isCancelled());
            try {
                long delay = reconnectTask.getDelay(TimeUnit.MILLISECONDS);
                status.put("remainingDelay", delay);
            } catch (Exception e) {
                status.put("remainingDelay", "unknown");
            }
        }
        return status;
    }

}