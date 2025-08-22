package top.redjujubetree.grpc.tunnel.payload;

import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class RegisterRequest{
	private String clientId;
	private String clientName;
	private String serverMachineName;
	private String clientVersion;
	private String clientIp;
	private String clientPlatform;
	private String clientDeviceId;
	Map<String, Object> metadata;

}
