package top.redjujubetree.demo.client.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.generator.SingleIpClientIdGenerator;

@Configuration
public class TunnelClientConfiguration {

	@Bean
	public ClientIdGenerator clientIdGenerator() {
		return new SingleIpClientIdGenerator();
	}

}