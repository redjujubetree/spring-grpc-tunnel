package top.redjujubetree.grpc.tunnel.server.listener;

public interface ClientConnectionCloseListener {
	/**
	 * This method is called when a client connection is closed.
	 *
	 * @param clientId The ID of the client whose connection was closed.
	 */
	void onClientConnectionClosed(String clientId);

	/**
	 * Get the priority order of this listener.
	 *
	 * @return The order value, lower values indicate higher priority.
	 */
	default int getOrder() {
		return 0;
	}
}
