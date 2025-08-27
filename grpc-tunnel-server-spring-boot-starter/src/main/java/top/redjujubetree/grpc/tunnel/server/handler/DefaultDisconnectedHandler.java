package top.redjujubetree.grpc.tunnel.server.handler;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.constant.ClientRequestTypes;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;

/**
 * Default implementation of DisconnectedHandler that handles DISCONNECT requests.
 * It logs the request data and returns null as a response.
 * This class can be extended to implement custom behavior for handling DISCONNECT requests.
 */
@Slf4j
public class DefaultDisconnectedHandler extends AbstractClientRequestMessageHandler implements DisconnectedHandler {

	@Override
	protected boolean supportRequestType(String request) {
		return ClientRequestTypes.DISCONNECT.equals(request);
	}

	@Override
	protected ResponsePayload handleRequest(RequestPayload request) {
		return null;
	}

	protected ResponsePayload handleRequest(TunnelMessage request) {
		String mapStr = request.getRequest().getData().toStringUtf8();
		log.info("handleRequest for disconnect : {}", mapStr);
		return null;
	}
}
