package top.redjujubetree.service;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.handler.DefaultConnectedHandler;

@Slf4j
@Component
public class ClientOnlineHandler extends DefaultConnectedHandler {

	@Override
	protected ResponsePayload handlerRequest(TunnelMessage request) {
		String mapStr = request.getRequest().getData().toStringUtf8();
		log.info("Server demo handlerRequest for connect : {}", mapStr);
		return null;
	}

}
