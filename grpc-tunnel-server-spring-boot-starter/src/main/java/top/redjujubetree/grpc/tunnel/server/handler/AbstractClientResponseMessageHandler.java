package top.redjujubetree.grpc.tunnel.server.handler;

import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractClientResponseMessageHandler implements MessageHandler {

	@Override
	public boolean support(TunnelMessage message) {
		ResponsePayload response = message.getResponse();
		return message.hasResponse() && MessageType.CLIENT_RESPONSE.equals(message.getType()) && supportsResponseType(response);
	}

	protected  boolean supportsResponseType(ResponsePayload request) {
		return supportsResponseType(request.getType());
	}

	protected abstract  boolean supportsResponseType(String request) ;


	@Override
	public CompletableFuture<TunnelMessage> handle(TunnelMessage request) {
		return CompletableFuture.supplyAsync(() -> {
			ResponsePayload responsePayload = handleResponse(request.getResponse());
			if (Objects.isNull(responsePayload)) {
				return null;
			}
			TunnelMessage.Builder builder = TunnelMessage.newBuilder()
					.setMessageId(UUID.randomUUID().toString())
					.setClientId(request.getClientId())
					.setType(MessageType.SERVER_RESPONSE)
					.setTimestamp(System.currentTimeMillis())
					.setCorrelationId(request.getMessageId())
					.setResponse(responsePayload);
			return builder.build();
		});
	}

	/**
	 * if the client does not need a response, return null
	 * @param response
	 * @return
	 */
	protected abstract ResponsePayload handleResponse(ResponsePayload response) ;


	@Override
	public int getOrder() {
		return MessageHandler.super.getOrder();
	}
}
