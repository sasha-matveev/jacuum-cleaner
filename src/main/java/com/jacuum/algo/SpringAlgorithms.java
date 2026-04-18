package com.jacuum.algo;

import org.springframework.context.ApplicationContext;
import java.util.List;
import java.util.Map;

/**
 * Discovers and instantiates {@link RobotAlgo} beans from the Spring context.
 * Beans must be annotated with {@link RobotAlgorithm}.
 */
public final class SpringAlgorithms implements Algorithms {

    private final ApplicationContext ctx;
    private final Map<String, Object> beans;

    public SpringAlgorithms(final ApplicationContext ctx) {
        this.ctx = ctx;
        this.beans = Map.copyOf(ctx.getBeansWithAnnotation(RobotAlgorithm.class));
    }

    @Override
    public List<String> names() {
        return this.beans.entrySet().stream()
            .map(e -> displayName(e.getValue()))
            .sorted()
            .toList();
    }

    @Override
    public RobotAlgo instantiate(final String name) throws Exception {
        for (final Map.Entry<String, Object> e : this.beans.entrySet()) {
            if (displayName(e.getValue()).equals(name)) {
                return (RobotAlgo) this.ctx.getBean(e.getKey());
            }
        }
        throw new Exception("Unknown algorithm: " + name);
    }

    private String displayName(final Object bean) {
        final RobotAlgorithm ann = bean.getClass().getAnnotation(RobotAlgorithm.class);
        final String v = ann.value();
        return v.isBlank() ? bean.getClass().getSimpleName() : v;
    }
}
