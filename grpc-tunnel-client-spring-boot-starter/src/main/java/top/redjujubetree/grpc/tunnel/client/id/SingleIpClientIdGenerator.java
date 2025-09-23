package top.redjujubetree.grpc.tunnel.client.id;

import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;
import top.redjujubetree.grpc.tunnel.utils.IpUtil;

public class SingleIpClientIdGenerator implements ClientIdGenerator {
	@Override
	public String generate(String clientName, TunnelProperties properties) {
		try {
			String ip = IpUtil.getBestIpv4();
			return ip.replace(".", "_");
		} catch (Exception e) {
			throw new RuntimeException("Unable to get local IPv4 address, generating random client ID", e);
		}
	}
}
