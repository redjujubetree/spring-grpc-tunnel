package top.redjujubetree.grpc.tunnel.handler;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.concurrent.CompletableFuture;

/**
 * message handler, use for handle tunnel message for both client and server.
 */
public interface MessageHandler {
    /**
     * if the handler supports the request type
     */
    boolean support(TunnelMessage request);

    /**
     * do handle the request,
     * if the CompletableFuture carries a result, it means the request is handled successfully,
     * and the result should be sent back to the request sender.
     * Otherwise, it means the request does not need a response.
     */
    CompletableFuture<TunnelMessage> handle(TunnelMessage request);
    
    /**
     * get the order of the handler, the smaller the order, the higher the priority.
     */
    default int getOrder() {
        return 0;
    }
}