package top.redjujubetree.grpc.tunnel.client.handler;

import top.redjujubetree.grpc.tunnel.handler.MessageHandler;
import top.redjujubetree.grpc.tunnel.proto.MessageType;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public abstract class AbstractServerRequestMessageHandler implements MessageHandler {

	@Override
	public boolean support(TunnelMessage message) {
		RequestPayload request = message.getRequest();
		return message.hasRequest() && MessageType.SERVER_REQUEST.equals(message.getType()) && supportRequestType(request);
	}

	protected  boolean supportRequestType(RequestPayload request) {
		return supportRequestType(request.getType());
	}

	protected abstract  boolean supportRequestType(String request) ;

	@Override
	public CompletableFuture<TunnelMessage> handle(TunnelMessage request) {
		return CompletableFuture.supplyAsync(() -> {
			ResponsePayload responsePayload = handleServerMessage(request);
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

	protected ResponsePayload handleServerMessage(TunnelMessage request) {
		return handleServerCommand(request.getRequest());
	}


	/**
	 * if the server does not need a response, return null
	 */
	protected ResponsePayload handleServerCommand(RequestPayload request) {
		throw new UnsupportedOperationException(this.getClass().getName()+" Not implemented handleServerCommand");
	}

	@Override
	public int getOrder() {
		return MessageHandler.super.getOrder();
	}
}
