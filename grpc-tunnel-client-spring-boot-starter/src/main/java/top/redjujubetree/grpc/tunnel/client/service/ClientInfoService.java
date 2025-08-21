package top.redjujubetree.grpc.tunnel.client.service;

import java.io.Serializable;
import java.util.Map;

public interface ClientInfoService {

	Map<String, Serializable> buildClentInfoPayload();
}
