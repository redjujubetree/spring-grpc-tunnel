package top.redjujubetree.grpc.tunnel.server.handler;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.constant.ClientRequestTypes;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;

/**
 * Default implementation of ConnectedHandler that handles CONNECT requests.
 * It logs the request data and returns null as a response.
 * This class can be extended to implement custom behavior for handling CONNECT requests.
 */
@Slf4j
public class DefaultConnectedHandler extends AbstractClientRequestMessageHandler implements ConnectedHandler {

	@Override
	protected boolean supportRequestType(String request) {
		return ClientRequestTypes.CONNECT.equals(request);
	}

	@Override
	protected ResponsePayload handleRequest(RequestPayload request) {
		String mapStr = request.getData().toStringUtf8();
		log.info("handleRequest for connect : {}", mapStr);
		return null;
	}

}
