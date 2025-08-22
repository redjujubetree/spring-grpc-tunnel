package top.redjujubetree.grpc.tunnel.server.connection;

import java.util.Collection;

public interface ClientManager{
    
    void addClient(ClientConnection connection);
    
    void removeClient(String clientId);
    void removeClient(String clientId, String reason);
    
    ClientConnection getClient(String clientId);

    Collection<ClientConnection> getAllClients();
    
}