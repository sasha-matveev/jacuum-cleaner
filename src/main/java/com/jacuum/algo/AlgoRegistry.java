package com.jacuum.algo;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Collects all {@link VacuumAlgo}-annotated {@link RobotAlgo} beans and makes them
 * available for the UI selector and game engine.
 */
@Component
public class AlgoRegistry {

    private final ApplicationContext ctx;

    public AlgoRegistry(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    /**
     * Returns a map of {@code algoId → display name} for all registered algorithms.
     * The algoId is the Spring bean name (class simple name, lowercased first char).
     */
    public Map<String, String> listAlgos() {
        Map<String, String> result = new LinkedHashMap<>();
        ctx.getBeansWithAnnotation(VacuumAlgo.class).forEach((beanName, bean) -> {
            VacuumAlgo annotation = bean.getClass().getAnnotation(VacuumAlgo.class);
            if (annotation != null) {
                result.put(beanName, annotation.value());
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns an instance of the algorithm with the given {@code algoId}.
     *
     * <p>Uses {@link ApplicationContext#getBean(String)} so that prototype-scoped
     * algorithms (e.g. stateful ones annotated with {@code @Scope("prototype")})
     * receive a <em>fresh instance per call</em>, while singleton-scoped algorithms
     * return the shared instance as usual.
     *
     * @throws IllegalArgumentException if no algorithm with this id is registered
     */
    public RobotAlgo getAlgo(String algoId) {
        try {
            Object bean = ctx.getBean(algoId);
            if (bean instanceof RobotAlgo algo) return algo;
            throw new IllegalArgumentException("Bean '" + algoId + "' does not implement RobotAlgo");
        } catch (NoSuchBeanDefinitionException e) {
            throw new IllegalArgumentException("Unknown algorithm: " + algoId, e);
        }
    }
}
