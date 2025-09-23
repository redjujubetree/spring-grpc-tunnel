package top.redjujubetree.grpc.tunnel.client.inject;

import java.lang.annotation.*;

/**
 * Annotation to inject GrpcTunnelClientService instances
 * Similar to @GrpcClient but for tunnel services
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrpcClientTunnel {
    
    /**
     * The name of the tunnel service to inject.
     * Can be either:
     * - Client name (e.g., "client-service1") - will automatically append "-tunnel"
     * - Full bean name (e.g., "client-service1-tunnel")
     * - Empty string for primary/default tunnel service
     * 
     * @return The tunnel service name
     */
    String value() default "";
    
    /**
     * Whether the injection is required.
     * If true, will fail if tunnel service is not found.
     * If false, will inject null if not found.
     * 
     * @return true if required, false otherwise
     */
    boolean required() default true;
    
    /**
     * Whether to use lazy injection.
     * If true, creates a proxy that resolves the service on first use.
     * 
     * @return true for lazy injection, false for immediate
     */
    boolean lazy() default false;
}