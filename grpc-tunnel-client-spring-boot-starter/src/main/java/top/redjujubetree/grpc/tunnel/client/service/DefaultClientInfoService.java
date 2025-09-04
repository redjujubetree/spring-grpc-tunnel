package top.redjujubetree.grpc.tunnel.client.service;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.utils.IpUtil;

import javax.annotation.Resource;
import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class DefaultClientInfoService implements ClientInfoService{

	@Resource
	private ClientIdGenerator clientIdGenerator;

	@Override
	public RegisterRequest buildClentInfoPayload() {
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setClientId(clientIdGenerator.generate());
		registerRequest.setServerMachineName(getComputerName());
		registerRequest.setClientPlatform(System.getProperty("os.name") + " " + System.getProperty("os.version"));
		registerRequest.setClientIp(IpUtil.getBestIpv4());
		return registerRequest;
	}

	public static String getComputerName() {
		String computerName = null;

		try {
			// 方法1：使用InetAddress
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// 方法2：使用环境变量
			computerName = System.getenv("COMPUTERNAME"); // Windows
			if (computerName == null) {
				computerName = System.getenv("HOSTNAME"); // Linux/Mac
			}
			if (computerName == null) {
				computerName = System.getenv("HOST"); // 其他系统
			}
		}

		return computerName != null ? computerName : "Unknown";
	}

}
