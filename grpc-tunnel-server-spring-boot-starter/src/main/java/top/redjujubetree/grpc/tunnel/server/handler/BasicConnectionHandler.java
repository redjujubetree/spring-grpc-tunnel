package top.redjujubetree.grpc.tunnel.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

public class BasicConnectionHandler implements ConnectionHandler {

    private static Logger log = LoggerFactory.getLogger(BasicConnectionHandler.class);
    @Override
    public ConnectionResult handleConnection(TunnelMessage message, RegisterRequest registerRequest) {
        String clientId = message.getClientId();

        if (!isValidClientId(clientId)) {
            return ConnectionResult.reject("Invalid client ID format");
        }
        log.info("BasicConnectionHandler: Handling connection for clientId: {}", clientId);
        return ConnectionResult.accept("Connection accepted");
    }
    
    private boolean isValidClientId(String clientId) {
        return clientId != null && !clientId.trim().isEmpty();
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}