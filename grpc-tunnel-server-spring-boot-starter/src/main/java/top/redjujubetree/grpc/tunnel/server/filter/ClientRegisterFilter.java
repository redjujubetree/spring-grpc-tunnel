package top.redjujubetree.grpc.tunnel.server.filter;

import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.handler.ConnectionResult;

/**
 * to handle the connection event, when the client connects to the server for the first time,
 * this handler will be invoked to process the connection message.
 */
public interface ClientRegisterFilter {

    /**
     * handle register event, when the client connects to the server for the first time.
     * @return true accept the client，false reject the client
     */
    ConnectionResult doFilter(TunnelMessage message, RegisterRequest registerRequest);


    /**
     * Get the priority order of this handler.
     */
    default int getOrder() {
        return 0;
    }
}

