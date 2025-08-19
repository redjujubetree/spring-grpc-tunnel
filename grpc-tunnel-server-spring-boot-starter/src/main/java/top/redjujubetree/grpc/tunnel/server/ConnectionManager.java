package top.redjujubetree.grpc.tunnel.server;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionManager {
    
    private final Map<String, ConnectionInfo> connections = new ConcurrentHashMap<>();
    
    public static class ConnectionInfo {
        private String clientId;
        private String ipAddress;
        private long connectedAt;
        private long lastActivityAt;
        private long messagesSent;
        private long messagesReceived;
        private String status;
        
        // constructors, getters and setters
        public ConnectionInfo(String clientId, String ipAddress) {
            this.clientId = clientId;
            this.ipAddress = ipAddress;
            this.connectedAt = System.currentTimeMillis();
            this.lastActivityAt = System.currentTimeMillis();
            this.messagesSent = 0;
            this.messagesReceived = 0;
            this.status = "CONNECTED";
        }
        
        public void incrementSent() {
            this.messagesSent++;
            this.lastActivityAt = System.currentTimeMillis();
        }
        
        public void incrementReceived() {
            this.messagesReceived++;
            this.lastActivityAt = System.currentTimeMillis();
        }
    }
    
    public void registerConnection(String clientId, String ipAddress) {
        connections.put(clientId, new ConnectionInfo(clientId, ipAddress));
    }
    
    public void unregisterConnection(String clientId) {
        ConnectionInfo info = connections.get(clientId);
        if (info != null) {
            info.status = "DISCONNECTED";
            connections.remove(clientId);
        }
    }
    
    public void recordMessageSent(String clientId) {
        ConnectionInfo info = connections.get(clientId);
        if (info != null) {
            info.incrementSent();
        }
    }
    
    public void recordMessageReceived(String clientId) {
        ConnectionInfo info = connections.get(clientId);
        if (info != null) {
            info.incrementReceived();
        }
    }
    
    public List<ConnectionInfo> getActiveConnections() {
        return new ArrayList<>(connections.values());
    }
    
    public Optional<ConnectionInfo> getConnection(String clientId) {
        return Optional.ofNullable(connections.get(clientId));
    }
    
    public int getActiveConnectionCount() {
        return connections.size();
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalConnections", connections.size());
        stats.put("totalMessagesSent", connections.values().stream()
            .mapToLong(c -> c.messagesSent).sum());
        stats.put("totalMessagesReceived", connections.values().stream()
            .mapToLong(c -> c.messagesReceived).sum());
        return stats;
    }
}