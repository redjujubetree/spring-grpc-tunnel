package top.redjujubetree.grpc.tunnel.client.service;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.client.GrpcTunnelClientService;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.utils.IpUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class DefaultClientInfoService implements ClientInfoService{

	@Override
	public RegisterRequest buildClientInfoPayload(GrpcTunnelClientService clientService){
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setServerMachineName(getComputerName());
		registerRequest.setClientName(clientService.getTunnelConfig().getClientName());
		registerRequest.setClientPlatform(System.getProperty("os.name") + " " + System.getProperty("os.version"));
		registerRequest.setClientIp(IpUtil.getBestIpv4());
		return registerRequest;
	}

	public static String getComputerName() {
		String computerName;

		try {
			// use InternetAddress to get the hostname
			computerName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// fallback to environment variables
			computerName = System.getenv("COMPUTERNAME"); // Windows
			if (computerName == null) {
				computerName = System.getenv("HOSTNAME"); // Linux/Mac
			}
			if (computerName == null) {
				computerName = System.getenv("HOST"); // Other Unix-like systems
			}
		}

		return computerName != null ? computerName : "Unknown";
	}

}
