package top.redjujubetree.grpc.tunnel.generator;

/**
 * ClientIdGenerator is a functional interface for generating unique client IDs.
 */
@FunctionalInterface
public interface ClientIdGenerator {
    String generate();
}