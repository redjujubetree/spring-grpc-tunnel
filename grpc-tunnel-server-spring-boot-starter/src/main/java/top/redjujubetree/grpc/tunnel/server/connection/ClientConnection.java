package top.redjujubetree.grpc.tunnel.server.connection;

import io.grpc.stub.StreamObserver;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Represents a client connection in the tunnel server.
 * This class holds information about the client, including its ID,
 * the observer for sending messages, connection time, last activity time,
 * and message counts (sent and received).
 */
public class ClientConnection {
    private final String clientId;
    private final StreamObserver<TunnelMessage> observer;
    private final long connectedAt;
    private volatile long lastActivity;
    private final AtomicLong messagesSent;
    private final AtomicLong messagesReceived;
    private Map<String, Object> metadata;

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
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    public Map<String, Object> getMetadata() { return metadata; }
    public String getClientId() { return clientId; }
    public StreamObserver<TunnelMessage> getObserver() { return observer; }
    public long getConnectedAt() { return connectedAt; }
    public long getLastActivity() { return lastActivity; }
    public long getMessagesSent() { return messagesSent.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
}