package top.redjujubeetree.utils;

import org.junit.jupiter.api.Test;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;
import top.redjujubetree.grpc.tunnel.utils.TunnelMessagesUtil;

public class TunnelMessageUtilTest {

	@Test
	public void testSerialize() {
		RegisterRequest registerRequest = new RegisterRequest();
		registerRequest.setClientName("client1");
		registerRequest.setClientPlatform("platform1");
		System.out.println(TunnelMessagesUtil.serializeObj(registerRequest));
	}
}
