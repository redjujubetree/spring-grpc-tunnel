package top.redjujubetree.grpc.tunnel.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 * bidirectional streaming service for establishing a tunnel
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.64.0)",
    comments = "Source: tunnel.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GrpcTunnelServiceGrpc {

  private GrpcTunnelServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "top.redjujubetree.tunnel.grpc.GrpcTunnelService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<top.redjujubetree.grpc.tunnel.proto.TunnelMessage,
      top.redjujubetree.grpc.tunnel.proto.TunnelMessage> getEstablishTunnelMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "EstablishTunnel",
      requestType = top.redjujubetree.grpc.tunnel.proto.TunnelMessage.class,
      responseType = top.redjujubetree.grpc.tunnel.proto.TunnelMessage.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<top.redjujubetree.grpc.tunnel.proto.TunnelMessage,
      top.redjujubetree.grpc.tunnel.proto.TunnelMessage> getEstablishTunnelMethod() {
    io.grpc.MethodDescriptor<top.redjujubetree.grpc.tunnel.proto.TunnelMessage, top.redjujubetree.grpc.tunnel.proto.TunnelMessage> getEstablishTunnelMethod;
    if ((getEstablishTunnelMethod = GrpcTunnelServiceGrpc.getEstablishTunnelMethod) == null) {
      synchronized (GrpcTunnelServiceGrpc.class) {
        if ((getEstablishTunnelMethod = GrpcTunnelServiceGrpc.getEstablishTunnelMethod) == null) {
          GrpcTunnelServiceGrpc.getEstablishTunnelMethod = getEstablishTunnelMethod =
              io.grpc.MethodDescriptor.<top.redjujubetree.grpc.tunnel.proto.TunnelMessage, top.redjujubetree.grpc.tunnel.proto.TunnelMessage>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "EstablishTunnel"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  top.redjujubetree.grpc.tunnel.proto.TunnelMessage.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  top.redjujubetree.grpc.tunnel.proto.TunnelMessage.getDefaultInstance()))
              .setSchemaDescriptor(new GrpcTunnelServiceMethodDescriptorSupplier("EstablishTunnel"))
              .build();
        }
      }
    }
    return getEstablishTunnelMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GrpcTunnelServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceStub>() {
        @java.lang.Override
        public GrpcTunnelServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GrpcTunnelServiceStub(channel, callOptions);
        }
      };
    return GrpcTunnelServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GrpcTunnelServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceBlockingStub>() {
        @java.lang.Override
        public GrpcTunnelServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GrpcTunnelServiceBlockingStub(channel, callOptions);
        }
      };
    return GrpcTunnelServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GrpcTunnelServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GrpcTunnelServiceFutureStub>() {
        @java.lang.Override
        public GrpcTunnelServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GrpcTunnelServiceFutureStub(channel, callOptions);
        }
      };
    return GrpcTunnelServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * bidirectional streaming service for establishing a tunnel
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * establish a tunnel with bidirectional streaming
     * </pre>
     */
    default io.grpc.stub.StreamObserver<top.redjujubetree.grpc.tunnel.proto.TunnelMessage> establishTunnel(
        io.grpc.stub.StreamObserver<top.redjujubetree.grpc.tunnel.proto.TunnelMessage> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getEstablishTunnelMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service GrpcTunnelService.
   * <pre>
   * bidirectional streaming service for establishing a tunnel
   * </pre>
   */
  public static abstract class GrpcTunnelServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return GrpcTunnelServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service GrpcTunnelService.
   * <pre>
   * bidirectional streaming service for establishing a tunnel
   * </pre>
   */
  public static final class GrpcTunnelServiceStub
      extends io.grpc.stub.AbstractAsyncStub<GrpcTunnelServiceStub> {
    private GrpcTunnelServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcTunnelServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GrpcTunnelServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * establish a tunnel with bidirectional streaming
     * </pre>
     */
    public io.grpc.stub.StreamObserver<top.redjujubetree.grpc.tunnel.proto.TunnelMessage> establishTunnel(
        io.grpc.stub.StreamObserver<top.redjujubetree.grpc.tunnel.proto.TunnelMessage> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getEstablishTunnelMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service GrpcTunnelService.
   * <pre>
   * bidirectional streaming service for establishing a tunnel
   * </pre>
   */
  public static final class GrpcTunnelServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<GrpcTunnelServiceBlockingStub> {
    private GrpcTunnelServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcTunnelServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GrpcTunnelServiceBlockingStub(channel, callOptions);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service GrpcTunnelService.
   * <pre>
   * bidirectional streaming service for establishing a tunnel
   * </pre>
   */
  public static final class GrpcTunnelServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<GrpcTunnelServiceFutureStub> {
    private GrpcTunnelServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GrpcTunnelServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GrpcTunnelServiceFutureStub(channel, callOptions);
    }
  }

  private static final int METHODID_ESTABLISH_TUNNEL = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_ESTABLISH_TUNNEL:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.establishTunnel(
              (io.grpc.stub.StreamObserver<top.redjujubetree.grpc.tunnel.proto.TunnelMessage>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getEstablishTunnelMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              top.redjujubetree.grpc.tunnel.proto.TunnelMessage,
              top.redjujubetree.grpc.tunnel.proto.TunnelMessage>(
                service, METHODID_ESTABLISH_TUNNEL)))
        .build();
  }

  private static abstract class GrpcTunnelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GrpcTunnelServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return top.redjujubetree.grpc.tunnel.proto.GrpcTunnelProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GrpcTunnelService");
    }
  }

  private static final class GrpcTunnelServiceFileDescriptorSupplier
      extends GrpcTunnelServiceBaseDescriptorSupplier {
    GrpcTunnelServiceFileDescriptorSupplier() {}
  }

  private static final class GrpcTunnelServiceMethodDescriptorSupplier
      extends GrpcTunnelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    GrpcTunnelServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GrpcTunnelServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GrpcTunnelServiceFileDescriptorSupplier())
              .addMethod(getEstablishTunnelMethod())
              .build();
        }
      }
    }
    return result;
  }
}
