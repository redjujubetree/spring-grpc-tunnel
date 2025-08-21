package top.redjujubetree.grpc.tunnel.server.handler;

import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * to handle the connection event, when the client connects to the server for the first time,
 * this handler will be invoked to process the connection message.
 */
public interface ConnectionHandler {

    /**
     * @return true accept the client，false reject the client
     */
    ConnectionResult handleConnection(TunnelMessage message, RegisterRequest registerRequest);
    
    /**
     * Get the priority order of this handler.
     */
    default int getOrder() {
        return 0;
    }
}

