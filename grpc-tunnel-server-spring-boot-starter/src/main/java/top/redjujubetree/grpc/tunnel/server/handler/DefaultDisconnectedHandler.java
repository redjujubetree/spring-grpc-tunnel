package top.redjujubetree.grpc.tunnel.server.handler;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.handler.ClientRequestMessageHandler;
import top.redjujubetree.grpc.tunnel.handler.DisconnectedHandler;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * Default implementation of DisconnectedHandler that handles DISCONNECT requests.
 * It logs the request data and returns null as a response.
 * This class can be extended to implement custom behavior for handling DISCONNECT requests.
 */
@Slf4j
public class DefaultDisconnectedHandler extends ClientRequestMessageHandler implements DisconnectedHandler {

	@Override
	public boolean supportsRequestType(RequestPayload request) {
		return "DISCONNECT".equals(request.getType());
	}

	@Override
	protected ResponsePayload handlerRequest(TunnelMessage request) {
		String mapStr = request.getRequest().getData().toStringUtf8();
		log.info("handlerRequest for disconnect : {}", mapStr);
		return null;
	}
}
