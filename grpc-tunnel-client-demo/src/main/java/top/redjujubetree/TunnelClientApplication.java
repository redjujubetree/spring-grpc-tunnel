package top.redjujubetree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.generator.SingleIpClientIdGenerator;

@SpringBootApplication(scanBasePackages = "top.redjujubetree")
public class TunnelClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(TunnelClientApplication.class, args);
	}


	@Bean
	public ClientIdGenerator clientIdGenerator() {
		return new SingleIpClientIdGenerator();
	}

}