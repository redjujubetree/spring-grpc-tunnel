package top.redjujubetree.demo.server.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.server.GrpcTunnelServerService;
import top.redjujubetree.grpc.tunnel.server.handler.DefaultConnectedHandler;

@Slf4j
@Component
public class ClientOnlineHandler extends DefaultConnectedHandler {

	@Autowired
	@Lazy
	private GrpcTunnelServerService grpcTunnelServerService;
	@Override
	protected ResponsePayload handleRequest(RequestPayload request) {
		String mapStr = request.getData().toStringUtf8();
		log.info("Server demo handleRequest for connect : {}", mapStr);
		return null;
	}

}
