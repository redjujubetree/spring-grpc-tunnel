package top.redjujubetree.demo.server.handler;

import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.server.handler.AbstractClientResponseMessageHandler;

@Component
public class ClientResponseAckHandler extends AbstractClientResponseMessageHandler{

	@Override
	protected boolean supportsResponseType(String request) {
		return "notifyWithAck".equals(request);
	}

	@Override
	protected ResponsePayload handleResponse(ResponsePayload response) {
		System.out.println("Received ack from client: " + response);
		return null;
	}
}
