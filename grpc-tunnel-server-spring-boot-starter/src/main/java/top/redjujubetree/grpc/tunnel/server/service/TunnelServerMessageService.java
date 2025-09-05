package top.redjujubetree.grpc.tunnel.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.connection.ClientConnection;
import top.redjujubetree.grpc.tunnel.server.connection.ConnectionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Slf4j
@Component
public class TunnelServerMessageService {

	@Autowired
	private ConnectionManager connectionManager;
	/**
	 * send a message to a specific client
	 */
	public boolean sendToClient(String clientId, TunnelMessage message) {
		if (clientId == null || message == null) {
			log.warn("Client ID or message is null, cannot send message");
			return false;
		}
		ClientConnection connection = connectionManager.getClient(clientId);
		if (connection != null) {
			try {
				boolean success = connection.sendMessage(message);
				if (success) {
					connectionManager.recordMessageSent(clientId);
					return true;
				} else {
					connectionManager.removeClient(clientId, "发送消息失败");
					return false;
				}
			} catch (Exception e) {
				log.error("Error sending message to client: {}", clientId, e);
				connectionManager.removeClient(clientId, "Error sending message");
				return false;
			}
		} else {
			log.warn("Client {} not found", clientId);
			return false;
		}
	}

	/**
	 * send a message to all connected clients
	 */
	public int broadcast(TunnelMessage message) {
		List<String> failedClients = new ArrayList<>();

		Collection<ClientConnection> allClients = connectionManager.getAllClients();
		for (ClientConnection connection : allClients) {
			boolean success = connection.sendMessage(message);
			if (success) {
				connectionManager.recordMessageSent(connection.getClientId());
			} else {
				log.error("Error broadcasting message to client: {}", connection.getClientId());
				failedClients.add(connection.getClientId());
			}
		}
		connectionManager.removeClients(failedClients, "Error broadcasting message");
		log.info("Broadcast message sent to {} clients, failed for {} clients",
				allClients.size() - failedClients.size(), failedClients.size());
		return allClients.size() - failedClients.size();
	}

}
