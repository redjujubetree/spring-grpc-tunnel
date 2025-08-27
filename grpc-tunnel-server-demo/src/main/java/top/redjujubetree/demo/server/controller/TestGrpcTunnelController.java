package top.redjujubetree.demo.server.controller;

import com.google.protobuf.ByteString;
import lombok.Setter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.GrpcTunnelServerService;

import javax.annotation.Resource;
import java.util.UUID;

@Setter
@RestController
@RequestMapping("/test/grpc/tunnel")
public class TestGrpcTunnelController {

	@Resource
	private GrpcTunnelServerService grpcTunnelServerService;
	@Resource
	private ClientIdGenerator clientIdGenerator;

	@RequestMapping("/notifyWithAck")
	public String notifyWithAck() {
		String clientId = clientIdGenerator.generate();
		TunnelMessage.Builder builder = TunnelMessage.newBuilder();
		builder.setClientId(clientId);
		builder.setMessageId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000").toString());
		RequestPayload.Builder payload = RequestPayload.newBuilder();
		payload.setType("notifyWithAck");
		payload.setData(ByteString.copyFromUtf8("hello client, this is a notify which need a ack from server"));
		builder.setRequest(payload.build());
		grpcTunnelServerService.sendToClient(clientId, builder.build());
		return "sent";
	}
}
