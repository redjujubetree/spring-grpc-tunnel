package top.redjujubetree.grpc.tunnel.generator;

import top.redjujubetree.grpc.tunnel.utils.IpUtil;

public class SingleIpClientIdGenerator implements ClientIdGenerator {
	@Override
	public String generate() {
		try {
			String ip = IpUtil.getBestIpv4();
			return ip.replace(".", "_");
		} catch (Exception e) {
			throw new RuntimeException("Unable to get local IPv4 address, generating random client ID", e);
		}
	}
}
