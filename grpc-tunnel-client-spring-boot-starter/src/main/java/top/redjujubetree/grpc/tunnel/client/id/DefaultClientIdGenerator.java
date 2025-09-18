package top.redjujubetree.grpc.tunnel.client.id;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;
import top.redjujubetree.grpc.tunnel.utils.IpUtil;

@Slf4j
public class DefaultClientIdGenerator implements ClientIdGenerator {
    
    @Override
    public String generate(String beanName, TunnelProperties properties) {
        return String.format("%s:%s", IpUtil.getBestIpv4().replace(".", "_"), beanName);
    }
}