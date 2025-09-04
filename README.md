# spring-grpc-tunnel
基于 `gRPC` 实现了一个简单的双向通信系统，支持客户端和服务端之间的消息互通。

客户端和服务端都可以发送和接收消息，适用于需要实时通信的应用场景。

服务端主要使用 `GrpcTunnelServerService` 处理逻辑, 客户主要使用 `GrpcTunnelClientService` 处理逻辑。

消息的传递通过 `GrpcTunnelMessage` 进行封装。

消息体为 `json` 格式数据, 如果需要更高效的数据传输,不适合使用默认的通道, 需要自己写 `gRPC` 服务. 具体可以参考 `https://github.com/Kenny-Tang/grpc-spring`. 本项目是基于该项目实现.

## 服务端消息推送

服务端可以通过 `GrpcTunnelServerService.sendToClient` 方法向指定客户端发送消息。

```java
```

## 客户端消息发送 
客户端可以通过 `GrpcTunnelClientService.sendRequest(String type, String data)` 向服务端发送消息.

```java
```


