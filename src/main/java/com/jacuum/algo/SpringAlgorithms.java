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

    public SpringAlgorithms(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public List<String> names() {
        return ctx.getBeansWithAnnotation(RobotAlgorithm.class)
            .entrySet().stream()
            .map(e -> displayName(e.getValue()))
            .sorted()
            .toList();
    }

    @Override
    public RobotAlgo instantiate(final String name) throws Exception {
        for (final Map.Entry<String, Object> e :
                ctx.getBeansWithAnnotation(RobotAlgorithm.class).entrySet()) {
            if (displayName(e.getValue()).equals(name)) {
                return (RobotAlgo) ctx.getBean(e.getKey());
            }
        }
        throw new Exception("Unknown algorithm: " + name);
    }

    private static String displayName(Object bean) {
        RobotAlgorithm ann = bean.getClass().getAnnotation(RobotAlgorithm.class);
        String v = ann.value();
        return v.isBlank() ? bean.getClass().getSimpleName() : v;
    }
}
