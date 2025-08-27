package top.redjujubetree.demo.server.handler;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.server.handler.DefaultConnectedHandler;

@Slf4j
@Component
public class ClientOnlineHandler extends DefaultConnectedHandler {

	@Override
	protected ResponsePayload handleRequest(RequestPayload request) {
		String mapStr = request.getData().toStringUtf8();
		log.info("Server demo handleRequest for connect : {}", mapStr);
		return null;
	}

}
