package top.redjujubetree.grpc.tunnel.server.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.handler.ConnectionResult;
import top.redjujubetree.grpc.tunnel.utils.JsonUtil;

public class BasicClientRegistrationFilter implements ClientRegisterFilter {

    private static Logger log = LoggerFactory.getLogger(BasicClientRegistrationFilter.class);
    @Override
    public ConnectionResult doFilter(TunnelMessage message, RegisterRequest registerRequest) {
        String clientId = message.getClientId();
        if (!isValidClientId(clientId)) {
            return ConnectionResult.reject("Invalid client ID format");
        }

        if (message == null || !message.hasRequest() || message.getRequest() == null) {
            return ConnectionResult.reject("Message or request is null");
        }
        log.info("BasicConnectionHandler: Received register request {} for clientId: {}", JsonUtil.toJson(registerRequest), clientId);
        if (registerRequest == null || registerRequest.getClientId() == null) {
            return ConnectionResult.reject("Register request or client ID is null");
        }

        if (!"CONNECT".equals(message.getRequest().getType())){
            return ConnectionResult.reject("Unsupported request type: " + message.getRequest().getType());
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