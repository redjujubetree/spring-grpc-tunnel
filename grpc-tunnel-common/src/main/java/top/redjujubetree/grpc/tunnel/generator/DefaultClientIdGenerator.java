package top.redjujubetree.grpc.tunnel.generator;

import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

@Slf4j
public class DefaultClientIdGenerator implements ClientIdGenerator {
    
    @Override
    public String generate() {
        try {
            String ip = InetAddress.getLocalHost().getHostAddress();
            String uuid = UUID.randomUUID().toString().substring(0, 8);
            return String.format("%s_%s_%d", ip.replace(".", "_"), uuid, 
                System.currentTimeMillis());
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unable to get local host address, generating random client ID", e);
        }
    }
}