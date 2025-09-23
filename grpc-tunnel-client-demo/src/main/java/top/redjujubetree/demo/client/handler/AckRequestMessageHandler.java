package top.redjujubetree.demo.client.handler;

import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.client.GrpcTunnelClientService;
import top.redjujubetree.grpc.tunnel.client.handler.AbstractServerRequestMessageHandler;
import top.redjujubetree.grpc.tunnel.client.inject.GrpcClientTunnel;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;

@Component
public class AckRequestMessageHandler extends AbstractServerRequestMessageHandler {

	@GrpcClientTunnel("tunnel-service1")
	private GrpcTunnelClientService grpcTunnelClientService;

	@Override
	protected boolean supportRequestType(String request) {
		return "notifyWithAck".equals(request);
	}

	@Override
	protected ResponsePayload handleServerCommand(RequestPayload request) {
		grpcTunnelClientService.sendOneWay("TEST_ONE_WAY_MESSAGE_FROM_CLIENT", "oneWayFromClient");
		ResponsePayload.Builder builder = ResponsePayload.newBuilder();
		builder.setCode(200).setType(request.getType()).setMessage("ack from client");
		return builder.build();
	}
}
