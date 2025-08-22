package top.redjujubetree.grpc.tunnel.server.handler;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.handler.ClientRequestMessageHandler;
import top.redjujubetree.grpc.tunnel.handler.ConnectedHandler;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * Default implementation of ConnectedHandler that handles CONNECT requests.
 * It logs the request data and returns null as a response.
 * This class can be extended to implement custom behavior for handling CONNECT requests.
 */
@Slf4j
public class DefaultConnectedHandler extends ClientRequestMessageHandler implements ConnectedHandler {


	@Override
	public boolean supportsRequestType(RequestPayload request) {
		return "CONNECT".equals(request.getType());
	}

	@Override
	protected ResponsePayload handlerRequest(TunnelMessage request) {
		String mapStr = request.getRequest().getData().toStringUtf8();
		log.info("handlerRequest for connect : {}", mapStr);
		return null;
	}

}
