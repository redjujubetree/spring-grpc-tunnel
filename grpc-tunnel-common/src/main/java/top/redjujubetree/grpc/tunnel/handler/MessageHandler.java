package top.redjujubetree.grpc.tunnel.handler;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import java.util.concurrent.CompletableFuture;

/**
 * message handler, use for handle tunnel message for both client and server.
 */
public interface MessageHandler {
    /**
     * if the handler supports the message type
     */
    boolean supports(TunnelMessage message);
    
    /**
     * do handle the message
     */
    CompletableFuture<TunnelMessage> handle(TunnelMessage message);
    
    /**
     * get the order of the handler, the smaller the order, the higher the priority.
     */
    default int getOrder() {
        return 0;
    }
}