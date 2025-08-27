package top.redjujubetree.demo.server.config;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.generator.SingleIpClientIdGenerator;
import top.redjujubetree.grpc.tunnel.proto.TunnelMessage;
import top.redjujubetree.grpc.tunnel.server.handler.HeartbeatHandler;

@Configuration
public class TunnelServerConfiguration {

	private Logger log = LoggerFactory.getLogger(this.getClass());
	@Bean
	public HeartbeatHandler heartbeatHandler() {
		return new HeartbeatHandler() {
			@Override
			public void handleHeartbeat(TunnelMessage message) {

			}

			@Override
			public void handleTimeout(String clientId) {
				log.info("Heartbeat timeout for client: {}", clientId);
				log.error("deal heartbeat timeout, need to set the client offline");
			}
		};
	}

	@Bean
	public ClientIdGenerator clientIdGenerator() {
		return new SingleIpClientIdGenerator();
	}

}