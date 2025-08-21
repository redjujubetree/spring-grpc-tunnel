package top.redjujubetree.grpc.tunnel.handler;

import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class ServerRequestMessageHandler implements MessageHandler{

	@Override
	public boolean supports(TunnelMessage message) {
		RequestPayload request = message.getRequest();
		return message.hasRequest() && MessageType.SERVER_REQUEST.equals(message.getType()) && supportsRequestType(request);
	}

	@Override
	public CompletableFuture<TunnelMessage> handle(TunnelMessage request) {
		return CompletableFuture.supplyAsync(() -> {
			ResponsePayload responsePayload = handlerServerCommand(request);
			if (Objects.isNull(responsePayload)) {
				return null;
			}
			TunnelMessage response = TunnelMessage.newBuilder()
					.setMessageId(UUID.randomUUID().toString())
					.setClientId(request.getClientId())
					.setType(MessageType.CLIENT_RESPONSE)
					.setTimestamp(System.currentTimeMillis())
					.setCorrelationId(request.getMessageId())
					.setResponse(responsePayload)
					.build();
			return response;
		});
	}

	/**
	 * if the server does not need a response, return null
	 * @param request
	 * @return
	 */
	protected abstract ResponsePayload handlerServerCommand(TunnelMessage request) ;

	@Override
	public int getOrder() {
		return MessageHandler.super.getOrder();
	}
}
