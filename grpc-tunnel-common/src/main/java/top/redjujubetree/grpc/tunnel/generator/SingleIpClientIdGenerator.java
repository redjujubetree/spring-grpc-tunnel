package top.redjujubetree.grpc.tunnel.generator;

import java.net.InetAddress;
import java.net.UnknownHostException;

import java.net.*;
import java.util.Enumeration;

public class SingleIpClientIdGenerator implements ClientIdGenerator {
	@Override
	public String generate() {
		try {
			String ip = getBestIpv4();
			return ip.replace(".", "_");
		} catch (Exception e) {
			throw new RuntimeException("Unable to get local IPv4 address, generating random client ID", e);
		}
	}

	private String getBestIpv4() throws SocketException, UnknownHostException {
		String localIp = null; // local IP will be used as fallback
		Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();

		while (nets.hasMoreElements()) {
			NetworkInterface netIf = nets.nextElement();
			if (netIf.isLoopback() || !netIf.isUp()) {
				continue;
			}
			Enumeration<InetAddress> addrs = netIf.getInetAddresses();
			while (addrs.hasMoreElements()) {
				InetAddress addr = addrs.nextElement();
				if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
					String ip = addr.getHostAddress();

					if (ip.startsWith("0.")){
						continue;
					}
					// public IPv4 addresses are preferred (not 10.x.x.x, 172.16-31.x.x, or 192.168.x.x)
					if (!isPrivateIp(ip)) {
						return ip;
					}

					// save the first private IP found
					if (localIp == null) {
						localIp = ip;
					}
				}
			}
		}

		// If no public IP found, return the first private IP or localhost
		return localIp != null ? localIp : InetAddress.getLocalHost().getHostAddress();
	}

	private boolean isPrivateIp(String ip) {
		return ip.startsWith("10.") ||
				ip.startsWith("192.168.") ||
				(ip.startsWith("172.") && inRange(ip, 16, 31));
	}

	private boolean inRange(String ip, int start, int end) {
		try {
			int second = Integer.parseInt(ip.split("\\.")[1]);
			return second >= start && second <= end;
		} catch (Exception e) {
			return false;
		}
	}
}
