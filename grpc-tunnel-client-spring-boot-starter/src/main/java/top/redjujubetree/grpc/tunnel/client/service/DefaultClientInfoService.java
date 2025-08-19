package top.redjujubetree.grpc.tunnel.client.service;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DefaultClientInfoService implements ClientInfoService{
	@Override
	public Object buildClentInfoPayload() {
		Map<String, Object> info = new HashMap<>();
		info.put("clientVersion", "1.0.0");
		info.put("clientType", "JAVA");
		info.put("timestamp", System.currentTimeMillis());
		info.put("serverMachineName", System.getProperty("os.name"));
		log.info("Client info payload: {}", info);
		return info;
	}
}
