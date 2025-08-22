package top.redjujubetree.grpc.tunnel.server.connection;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.server.listener.ClientConnectionCloseListener;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
public class ConnectionManager implements ClientManager {
    
    private final Map<String, ClientConnection> connections = new ConcurrentHashMap<>();
    private final ReadWriteLock statisticsLock = new ReentrantReadWriteLock();
    
    // statistics
    private long totalConnectionsEver = 0;
    private long totalDisconnections = 0;

    List<ClientConnectionCloseListener> clientConnectionCloseListeners;

    public ConnectionManager() {
        this.clientConnectionCloseListeners = new ArrayList<>();
    }
    public ConnectionManager(List<ClientConnectionCloseListener> clientConnectionCloseListeners) {
        this.clientConnectionCloseListeners = clientConnectionCloseListeners != null ?
                new ArrayList<>(clientConnectionCloseListeners) : new ArrayList<>();
    }
    @Override
    public void addClient(ClientConnection connection) {
        if (connection == null || connection.getClientId() == null) {
            throw new IllegalArgumentException("Connection and clientId cannot be null");
        }
        
        // if the client already exists, replace it
        ClientConnection oldConnection = connections.put(connection.getClientId(), connection);
        if (oldConnection != null) {
            log.warn("Replacing existing connection for client: {}", connection.getClientId());
            closeConnection(oldConnection);
        }
        
        statisticsLock.writeLock().lock();
        try {
            totalConnectionsEver++;
        } finally {
            statisticsLock.writeLock().unlock();
        }
        
        log.info("Client connected: {} (Total active clients: {})", 
                connection.getClientId(), connections.size());
    }

    @Override
    public void removeClient(String clientId) {
        removeClient(clientId, "Unspecified reason");
    }

    @Override
    public void removeClient(String clientId, String reason) {
        if (clientId == null) {
            return;
        }
        
        ClientConnection connection = connections.remove(clientId);
        if (connection != null) {
            statisticsLock.writeLock().lock();
            try {
                totalDisconnections++;
            } finally {
                statisticsLock.writeLock().unlock();
            }
            
            log.info("Client removed: {} (Reason: {}, Total active clients: {})", 
                    clientId, reason, connections.size());
            closeConnection(connection);
        }
    }

    @Override
    public ClientConnection getClient(String clientId) {
        return connections.get(clientId);
    }

    @Override
    public Collection<ClientConnection> getAllClients() {
        return new ArrayList<>(connections.values());
    }
    
    public Set<String> getAllClientIds() {
        return new HashSet<>(connections.keySet());
    }
    
    /**
     * batch remove clients by clientIDs
     */
    public void removeClients(Collection<String> clientIds, String reason) {
        if (clientIds == null || clientIds.isEmpty()) {
            return;
        }
        
        for (String clientId : clientIds) {
            removeClient(clientId, reason);
        }
    }
    
    /**
     * check if a client is connected
     */
    public boolean hasClient(String clientId) {
        return connections.containsKey(clientId);
    }
    
    /**
     * get active connection count
     */
    public int getActiveConnectionCount() {
        return connections.size();
    }
    
    /**
     * get a specific client connection by clientId
     */
    public Optional<ClientConnection> getConnection(String clientId) {
        return Optional.ofNullable(connections.get(clientId));
    }
    
    /**
     * update the last activity time of a client connection
     */
    public void recordMessageSent(String clientId) {
        ClientConnection connection = connections.get(clientId);
        if (connection != null) {
            connection.incrementSent();
            connection.updateLastActivity();
        }
    }
    
    /**
     * update the last activity time of a client connection
     */
    public void recordMessageReceived(String clientId) {
        ClientConnection connection = connections.get(clientId);
        if (connection != null) {
            connection.incrementReceived();
            connection.updateLastActivity();
        }
    }
    
    /**
     * Get statistics about the connection manager.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        statisticsLock.readLock().lock();
        try {
            stats.put("activeConnections", connections.size());
            stats.put("totalConnectionsEver", totalConnectionsEver);
            stats.put("totalDisconnections", totalDisconnections);
        } finally {
            statisticsLock.readLock().unlock();
        }
        
        long totalMessagesSent = 0;
        long totalMessagesReceived = 0;
        long oldestConnection = System.currentTimeMillis();
        
        for (ClientConnection conn : connections.values()) {
            totalMessagesSent += conn.getMessagesSent();
            totalMessagesReceived += conn.getMessagesReceived();
            oldestConnection = Math.min(oldestConnection, conn.getConnectedAt());
        }
        
        stats.put("totalMessagesSent", totalMessagesSent);
        stats.put("totalMessagesReceived", totalMessagesReceived);
        if (!connections.isEmpty()) {
            stats.put("oldestConnectionAge", System.currentTimeMillis() - oldestConnection);
        }
        
        return stats;
    }
    
    /**
     * Get a list of inactive clients based on the last activity time.
     * @param timeout milliseconds of inactivity to consider a client inactive
     * @return list of client IDs that have been inactive for longer than the specified timeout
     */
    public List<String> getInactiveClients(long timeout) {
        List<String> inactiveClients = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, ClientConnection> entry : connections.entrySet()) {
            if (currentTime - entry.getValue().getLastActivity() > timeout) {
                inactiveClients.add(entry.getKey());
            }
        }
        return inactiveClients;
    }
    
    /**
     * Shut down the ConnectionManager and close all connections.
     */
    public void shutdown() {
        log.info("Shutting down ConnectionManager, closing {} connections", connections.size());
        
        for (ClientConnection connection : connections.values()) {
            closeConnection(connection);
        }
        
        connections.clear();
    }
    
    /**
     * close a specific client connection
     */
    private void closeConnection(ClientConnection connection) {
        if (connection == null) {
            return;
        }
        
        try {
            connection.getObserver().onCompleted();
            for (ClientConnectionCloseListener clientConnectionCloseListener : clientConnectionCloseListeners) {
                try {
                    clientConnectionCloseListener.onClientConnectionClosed(connection.getClientId());
                } catch (Exception e) {
                    log.error("Error notifying listener {} about closed connection for client: {}",
                            clientConnectionCloseListener.getClass().getSimpleName(), connection.getClientId(), e);
                }
            }
        } catch (Exception e) {
            // the connection might already be closed
            log.debug("Error closing connection for client: {}", connection.getClientId(), e);
        }
    }
    
}