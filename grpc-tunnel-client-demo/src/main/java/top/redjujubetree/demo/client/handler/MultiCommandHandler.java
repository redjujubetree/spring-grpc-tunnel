package top.redjujubetree.demo.client.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.client.handler.AbstractServerRequestMessageHandler;
import top.redjujubetree.grpc.tunnel.proto.RequestPayload;
import top.redjujubetree.grpc.tunnel.proto.ResponsePayload;
import top.redjujubetree.grpc.tunnel.utils.TunnelMessagesUtil;

@Slf4j
@Component
public class MultiCommandHandler extends AbstractServerRequestMessageHandler {
	@Override
	protected boolean supportRequestType(String request) {
		if (ServerTypes.TYPE_ECHO.equals(request)) {
			return true;
		}
		if (ServerTypes.TYPE_TIME.equals(request)) {
			return true;
		}
		if (ServerTypes.TYPE_UPPERCASE.equals(request)) {
			return true;
		}
		return false;
	}

	@Override
	protected ResponsePayload handleServerCommand(RequestPayload request) {
		if (ServerTypes.TYPE_ECHO.equals(request.getType())) {
			String s = TunnelMessagesUtil.deserializeRequest(request, String.class);
			log.info("Echo command received with data: {}", s);
			ResponsePayload.Builder builder = ResponsePayload.newBuilder();
			builder.setCode(200).setType(request.getType()).setMessage("Echo: " + request.getData().toStringUtf8());
			return builder.build();
		}
		if (ServerTypes.TYPE_TIME.equals(request.getType())) {
			String s = TunnelMessagesUtil.deserializeRequest(request, String.class);
			log.info("Time command received with data: {}", s);
			ResponsePayload.Builder builder = ResponsePayload.newBuilder();
			builder.setCode(200).setType(request.getType()).setMessage("Time: " + System.currentTimeMillis());
			return builder.build();
		}
		if (ServerTypes.TYPE_UPPERCASE.equals(request.getType())) {
			String s = TunnelMessagesUtil.deserializeRequest(request, String.class);
			log.info("Uppercase command received with data: {}", s);
			ResponsePayload.Builder builder = ResponsePayload.newBuilder();
			builder.setCode(200).setType(request.getType()).setMessage("Uppercase: " + s.toUpperCase());
			return builder.build();
		}
		return null;
	}
}
