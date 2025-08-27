package top.redjujubetree.demo.server.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.redjujubetree.grpc.tunnel.server.listener.ClientConnectionCloseListener;

@Slf4j
@Service
public class ClientOfflineHandler implements ClientConnectionCloseListener {

	@Override
	public void onClientConnectionClosed(String clientId) {
		log.info("Client connection closed: {}", clientId);
		handle(clientId);
	}

	private void handle(String clientId) {
		log.info("Handling client offline logic for client: {}", clientId);
		log.info("Client {} is offline, performing necessary cleanup and notifications.", clientId);
	}

}
