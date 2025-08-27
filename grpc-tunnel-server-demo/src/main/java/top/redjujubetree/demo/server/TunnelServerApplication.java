package top.redjujubetree.demo.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(
    scanBasePackages = "top.redjujubetree.demo.server"
)
public class TunnelServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TunnelServerApplication.class, args);
	}

}