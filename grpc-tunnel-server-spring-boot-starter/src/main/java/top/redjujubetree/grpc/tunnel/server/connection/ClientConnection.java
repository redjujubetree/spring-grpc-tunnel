package top.redjujubetree.grpc.tunnel.server.connection;

import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents a client connection in the tunnel server.
 * This class holds information about the client, including its ID,
 * the observer for sending messages, connection time, last activity time,
 * and message counts (sent and received).
 */
public class ClientConnection {
    
    private static final Logger log = LoggerFactory.getLogger(ClientConnection.class);
    
    private final String clientId;
    private final StreamObserver<TunnelMessage> observer;
    private final long connectedAt;
    private volatile long lastActivity;
    private final AtomicLong messagesSent;
    private final AtomicLong messagesReceived;
    private Map<String, Object> metadata;
    
    private final ReentrantLock sendLock = new ReentrantLock();

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
    
    public boolean sendMessage(TunnelMessage message) {
        if (message == null) {
            log.warn("can not send null message - ClientId: {}", clientId);
            return false;
        }
        
        sendLock.lock();
        try {
            observer.onNext(message);
            messagesSent.incrementAndGet();
            updateLastActivity();
            
            log.debug("message send success - ClientId: {}, MessageId: {}",
                clientId, message.getMessageId());
            return true;
            
        } catch (Exception e) {
            log.error("发送消息失败 - ClientId: {}, MessageId: {}", 
                clientId, message.getMessageId(), e);
            return false;
        } finally {
            sendLock.unlock();
        }
    }
    
    public void closeConnection() {
        sendLock.lock();
        try {
            observer.onCompleted();
            log.info("Connection closed - ClientId: {}", clientId);
        } catch (Exception e) {
            log.debug("error on closing - ClientId: {}", clientId, e);
        } finally {
            sendLock.unlock();
        }
    }
    
    public void closeConnectionWithError(Throwable error) {
        sendLock.lock();
        try {
            observer.onError(error);
            log.info("closeConnectionWithError - ClientId: {}, Error: {}", clientId, error.getMessage());
        } catch (Exception e) {
            log.debug("closeConnectionWithError - ClientId: {}", clientId, e);
        } finally {
            sendLock.unlock();
        }
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
    public Map<String, Object> getMetadata() { return metadata; }
    public String getClientId() { return clientId; }
    
    public long getConnectedAt() { return connectedAt; }
    public long getLastActivity() { return lastActivity; }
    public long getMessagesSent() { return messagesSent.get(); }
    public long getMessagesReceived() { return messagesReceived.get(); }
    

    public int getQueueLength() {
        return sendLock.getQueueLength();
    }
}