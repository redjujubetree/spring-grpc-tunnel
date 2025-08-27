package top.redjujubetree.demo.client.handler;

import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.client.handler.AbstractServerRequestMessageHandler;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;

@Component
public class AckRequestMessageHandler extends AbstractServerRequestMessageHandler {

	@Override
	protected boolean supportRequestType(String request) {
		return "notifyWithAck".equals(request);
	}

	@Override
	protected ResponsePayload handleServerCommand(RequestPayload request) {
		ResponsePayload.Builder builder = ResponsePayload.newBuilder();
		builder.setCode(200).setType(request.getType()).setMessage("ack from client");
		return builder.build();
	}
}
