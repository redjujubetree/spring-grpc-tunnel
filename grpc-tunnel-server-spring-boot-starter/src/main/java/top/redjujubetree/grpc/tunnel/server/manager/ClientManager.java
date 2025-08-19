package top.redjujubetree.grpc.tunnel.server.manager;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Collection;

public interface ClientManager{
    
    void addClient(ClientConnection connection);
    
    void removeClient(String clientId);
    
    ClientConnection getClient(String clientId);

    Collection<ClientConnection> getAllClients();
    
    void pushMessage(String clientId, TunnelMessage message) ;
    
    void broadcast(TunnelMessage message) ;
}