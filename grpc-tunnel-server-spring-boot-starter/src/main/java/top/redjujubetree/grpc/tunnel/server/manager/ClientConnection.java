package top.redjujubetree.grpc.tunnel.server.manager;

import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import io.grpc.stub.StreamObserver;

public class ClientConnection {
	private String clientId;
	private StreamObserver<TunnelMessage> observer;

	public ClientConnection(String clientId, StreamObserver<TunnelMessage> observer) {
		this.clientId = clientId;
		this.observer = observer;
	}

	public void close() {
		if (observer != null) {
			observer.onCompleted();
		}
	}

	public String getClientId() {
		return clientId;
	}

	public void sendMessage(TunnelMessage message) {
		if (observer != null) {
			observer.onNext(message);
		}
	}
}
