package top.redjujubetree.grpc.tunnel.client.id;

import lombok.extern.slf4j.Slf4j;
import top.redjujubetree.grpc.tunnel.client.config.TunnelProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Slf4j
public class DefaultClientIdGenerator implements ClientIdGenerator {
    
    @Override
    public String generate(String beanName, TunnelProperties properties) {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            return String.format("%s:%s", ip.replace(".", "_"), beanName);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to get local host address, generating random client ID", e);
        }
    }
}