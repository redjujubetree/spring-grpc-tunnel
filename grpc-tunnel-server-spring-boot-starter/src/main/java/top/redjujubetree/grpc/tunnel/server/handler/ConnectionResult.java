package top.redjujubetree.grpc.tunnel.server.handler;

import java.util.HashMap;
import java.util.Map;

public class ConnectionResult {
    private final boolean accepted;
    private final String message;
    private final Map<String, Object> metadata;
    
    private ConnectionResult(boolean accepted, String message, Map<String, Object> metadata) {
        this.accepted = accepted;
        this.message = message;
        this.metadata = metadata;
    }
    
    public static ConnectionResult accept(String message) {
        return new ConnectionResult(true, message, new HashMap<>());
    }
    
    public static ConnectionResult reject(String message) {
        return new ConnectionResult(false, message, new HashMap<>());
    }
    
    public static ConnectionResult acceptWithMetadata(String message, Map<String, Object> metadata) {
        return new ConnectionResult(true, message, metadata);
    }
    
    // getters...
    public boolean isAccepted() { return accepted; }
    public String getMessage() { return message; }
    public Map<String, Object> getMetadata() { return metadata; }
}