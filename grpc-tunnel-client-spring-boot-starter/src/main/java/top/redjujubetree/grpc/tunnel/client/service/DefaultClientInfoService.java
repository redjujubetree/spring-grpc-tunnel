package top.redjujubetree.grpc.tunnel.client.service;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.generator.ClientIdGenerator;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.utils.IpUtil;

import javax.annotation.Resource;

@Slf4j
public class DefaultClientInfoService implements ClientInfoService{

	@Resource
	private ClientIdGenerator clientIdGenerator;
	@Override
	public RegisterRequest buildClentInfoPayload() {
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setClientId(clientIdGenerator.generate());
		registerRequest.setServerMachineName(System.getProperty("os.name"));
		registerRequest.setClientPlatform(System.getProperty("os.name") + " " + System.getProperty("os.version"));
		registerRequest.setClientIp(IpUtil.getBestIpv4());
		return registerRequest;
	}
}
