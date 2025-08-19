package top.redjujubetree;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "top.redjujubetree.service")
public class TunnelServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TunnelServerApplication.class, args);
	}

}