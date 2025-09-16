package top.redjujubetree.grpc.tunnel.client.id;

import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;

public interface ClientIdGenerator {
    String generate(String clientName, TunnelProperties properties);
}