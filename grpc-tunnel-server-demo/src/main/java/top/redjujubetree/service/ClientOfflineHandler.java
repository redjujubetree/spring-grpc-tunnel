package top.redjujubetree.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import top.redjujubetree.grpc.tunnel.server.listener.ClientConnectionCloseListener;

@Slf4j
@Service
public class ClientOfflineHandler implements ClientConnectionCloseListener {

	@Override
	public void onClientConnectionClosed(String clientId) {
		log.info("Client connection closed: {}", clientId);
		handler(clientId);
	}

	private void handler(String clientId) {
		log.info("Handling client offline logic for client: {}", clientId);
		log.info("Client {} is offline, performing necessary cleanup and notifications.", clientId);
	}

}
