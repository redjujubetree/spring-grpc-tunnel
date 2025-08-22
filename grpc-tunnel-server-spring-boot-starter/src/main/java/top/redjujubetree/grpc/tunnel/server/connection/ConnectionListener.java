package top.redjujubetree.grpc.tunnel.server.connection;

// 连接监听器接口
public interface ConnectionListener {
    void onConnected();
    void onDisconnected();
}