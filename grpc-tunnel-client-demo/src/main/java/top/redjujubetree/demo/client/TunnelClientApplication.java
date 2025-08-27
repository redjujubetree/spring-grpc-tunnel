package top.redjujubetree.demo.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "top.redjujubetree")
public class TunnelClientApplication {

	public static void main(String[] args) {
		SpringApplication.run(TunnelClientApplication.class, args);
	}
}