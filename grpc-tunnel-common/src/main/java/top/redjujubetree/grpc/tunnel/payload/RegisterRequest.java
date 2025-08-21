package top.redjujubetree.grpc.tunnel.payload;

import lombok.Data;

import java.util.Map;

@Data
public class RegisterRequest{
	private String clientId;
	private String clientName;
	private String clientVersion;
	private String clientPlatform;
	private String clientDeviceId;
	Map<String, Object> metadata;
}
