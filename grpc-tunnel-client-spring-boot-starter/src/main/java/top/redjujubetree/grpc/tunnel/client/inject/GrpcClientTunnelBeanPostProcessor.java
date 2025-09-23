package top.redjujubetree.grpc.tunnel.client.inject;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Objects.requireNonNull;

/**
 * BeanPostProcessor for @GrpcClientTunnel injection with delayed injection support
 */
@Slf4j
public class GrpcClientTunnelBeanPostProcessor implements BeanPostProcessor, ApplicationContextAware, ApplicationListener<ContextRefreshedEvent> {

    private final ApplicationContext applicationContext;
    
    // Cache injection points that need to be processed later
    private final List<PendingInjection> pendingInjections = new ArrayList<>();
    
    // Track processed beans to avoid duplicate processing
    private final Map<Object, List<InjectionPoint>> processedBeans = new ConcurrentHashMap<>();

    public GrpcClientTunnelBeanPostProcessor(final ApplicationContext applicationContext) {
        this.applicationContext = requireNonNull(applicationContext, "applicationContext");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // ApplicationContext is already set in constructor
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        Class<?> clazz = bean.getClass();
        List<InjectionPoint> injectionPoints = findInjectionPoints(clazz);
        
        if (!injectionPoints.isEmpty()) {
            // Try immediate injection, but save for later if it fails
            for (InjectionPoint injectionPoint : injectionPoints) {
                boolean injected = tryInjectField(bean, injectionPoint);
                if (!injected) {
                    // Save for delayed injection
                    pendingInjections.add(new PendingInjection(bean, beanName, injectionPoint));
                    log.debug("Deferred injection for {} in bean {}", injectionPoint.member, beanName);
                }
            }
            
            // Track this bean for potential re-injection
            processedBeans.put(bean, injectionPoints);
        }
        
        return bean;
    }

    /**
     * Find all injection points in the given class
     */
    private List<InjectionPoint> findInjectionPoints(Class<?> clazz) {
        List<InjectionPoint> injectionPoints = new ArrayList<>();
        Class<?> targetClass = clazz;
        
        do {
            // Process fields
            for (Field field : targetClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                
                GrpcClientTunnel annotation = AnnotationUtils.findAnnotation(field, GrpcClientTunnel.class);
                if (annotation != null) {
                    injectionPoints.add(new InjectionPoint(field, annotation));
                }
            }
            
            // Process setter methods
            for (Method method : targetClass.getDeclaredMethods()) {
                if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1) {
                    continue;
                }
                
                GrpcClientTunnel annotation = AnnotationUtils.findAnnotation(method, GrpcClientTunnel.class);
                if (annotation != null) {
                    injectionPoints.add(new InjectionPoint(method, annotation));
                }
            }
            
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);
        
        return injectionPoints;
    }

    /**
     * Try to inject a field/method, return true if successful
     */
    private boolean tryInjectField(Object bean, InjectionPoint injectionPoint) {
        try {
            Object tunnelService = resolveTunnelService(injectionPoint);
            
            if (tunnelService != null) {
                if (injectionPoint.member instanceof Field) {
                    Field field = (Field) injectionPoint.member;
                    ReflectionUtils.makeAccessible(field);
                    ReflectionUtils.setField(field, bean, tunnelService);
                } else if (injectionPoint.member instanceof Method) {
                    Method method = (Method) injectionPoint.member;
                    ReflectionUtils.makeAccessible(method);
                    ReflectionUtils.invokeMethod(method, bean, tunnelService);
                }
                return true;
            } else if (!injectionPoint.annotation.required()) {
                // Optional injection, null is acceptable
                return true;
            }
            
            return false;
        } catch (Exception e) {
            if (injectionPoint.annotation.required()) {
                log.debug("Failed to inject required tunnel service for {}: {}", 
                         injectionPoint.member, e.getMessage());
            }
            return false;
        }
    }

    /**
     * Resolve tunnel service for injection
     */
    private Object resolveTunnelService(InjectionPoint injectionPoint) {
        String tunnelName = injectionPoint.annotation.value();
        Class<?> targetType = getTargetType(injectionPoint.member);
        
        try {
            // Case 1: Specific name provided
            if (StringUtils.hasText(tunnelName)) {
                // Try standard bean name first
                if (applicationContext.containsBean(tunnelName)) {
                    return applicationContext.getBean(tunnelName, targetType);
                }
                
                // Try with "-tunnel" suffix
                String tunnelBeanName = tunnelName + "-tunnel";
                if (applicationContext.containsBean(tunnelBeanName)) {
                    return applicationContext.getBean(tunnelBeanName, targetType);
                }
                
                // Try without "-tunnel" suffix if it already has it
                if (tunnelName.endsWith("-tunnel")) {
                    String clientName = tunnelName.substring(0, tunnelName.length() - 7);
                    if (applicationContext.containsBean(clientName)) {
                        Object bean = applicationContext.getBean(clientName);
                        if (targetType.isInstance(bean)) {
                            return bean;
                        }
                    }
                }
            }
            
            // Case 2: No name specified, try to find default/primary
            else {
                // Try to get the primary bean
                if (applicationContext.containsBean("grpcTunnelClientService")) {
                    return applicationContext.getBean("grpcTunnelClientService", targetType);
                }
                
                // Get all beans of the target type
                Map<String, ?> beansOfType = applicationContext.getBeansOfType(targetType);
                
                if (beansOfType.size() == 1) {
                    // Only one bean, use it
                    return beansOfType.values().iterator().next();
                } else if (beansOfType.isEmpty()) {
                    // No beans found
                    return null;
                } else {
                    // Multiple beans, need to specify which one
                    log.warn("Multiple tunnel services found for type {}. Please specify the name using @GrpcClientTunnel(\"name\")", 
                            targetType.getSimpleName());
                    return null;
                }
            }
            
        } catch (NoSuchBeanDefinitionException e) {
            log.debug("Tunnel service not found: {} for type {}", tunnelName, targetType.getSimpleName());
        } catch (Exception e) {
            log.debug("Error resolving tunnel service: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Get target type from member (field or method)
     */
    private Class<?> getTargetType(Member member) {
        if (member instanceof Field) {
            return ((Field) member).getType();
        } else if (member instanceof Method) {
            return ((Method) member).getParameterTypes()[0];
        }
        throw new IllegalArgumentException("Unknown member type: " + member.getClass());
    }

    /**
     * Handle context refreshed event - retry pending injections
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!pendingInjections.isEmpty()) {
            log.info("Processing {} pending tunnel injections", pendingInjections.size());
            
            List<PendingInjection> stillPending = new ArrayList<>();
            
            for (PendingInjection pending : pendingInjections) {
                boolean injected = tryInjectField(pending.bean, pending.injectionPoint);
                
                if (!injected && pending.injectionPoint.annotation.required()) {
                    // Still couldn't inject a required field
                    stillPending.add(pending);
                } else if (injected) {
                    log.debug("Successfully injected {} in bean {}", 
                             pending.injectionPoint.member, pending.beanName);
                }
            }
            
            // Report any failed required injections
            if (!stillPending.isEmpty()) {
                for (PendingInjection pending : stillPending) {
                    String tunnelName = pending.injectionPoint.annotation.value();
                    String message = StringUtils.hasText(tunnelName) 
                        ? String.format("No tunnel service found with name '%s' for injection point %s in bean %s", 
                                       tunnelName, pending.injectionPoint.member, pending.beanName)
                        : String.format("No tunnel service found for injection point %s in bean %s. " +
                                       "Multiple services may exist - please specify using @GrpcClientTunnel(\"name\")", 
                                       pending.injectionPoint.member, pending.beanName);
                    
                    log.error(message);
                    throw new BeanCreationException(pending.beanName, message);
                }
            }
            
            // Clear pending injections
            pendingInjections.clear();
        }
    }

    /**
     * Injection point information
     */
    private static class InjectionPoint {
        final Member member;
        final GrpcClientTunnel annotation;
        
        InjectionPoint(Member member, GrpcClientTunnel annotation) {
            this.member = member;
            this.annotation = annotation;
        }
    }

    /**
     * Pending injection information
     */
    private static class PendingInjection {
        final Object bean;
        final String beanName;
        final InjectionPoint injectionPoint;
        
        PendingInjection(Object bean, String beanName, InjectionPoint injectionPoint) {
            this.bean = bean;
            this.beanName = beanName;
            this.injectionPoint = injectionPoint;
        }
    }
}