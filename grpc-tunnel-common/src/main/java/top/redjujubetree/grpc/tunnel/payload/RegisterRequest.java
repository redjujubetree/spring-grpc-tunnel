package top.redjujubetree.grpc.tunnel.payload;

import lombok.Data;

@Data
public class RegisterRequest{
	private String clientId;
	private String clientName;
	private String clientVersion;
	private String clientPlatform;
	private String clientDeviceId;
}
