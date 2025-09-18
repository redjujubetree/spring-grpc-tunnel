package top.redjujubetree.grpc.tunnel.client.service;

import top.redjujubetree.grpc.tunnel.client.GrpcTunnelClientService;
import top.redjujubetree.grpc.tunnel.payload.RegisterRequest;

public interface ClientInfoService {

	RegisterRequest buildClientInfoPayload(GrpcTunnelClientService clientService);
}
