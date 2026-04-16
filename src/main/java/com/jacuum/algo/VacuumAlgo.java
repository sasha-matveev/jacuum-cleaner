package com.jacuum.algo;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a class as a registered vacuum robot algorithm.
 *
 * <p>Any class annotated with {@code @VacuumAlgo} and implementing {@link RobotAlgo}
 * will be auto-discovered by {@link AlgoRegistry} and made available in the UI.
 *
 * <p>Example:
 * <pre>{@code
 * @VacuumAlgo("My Smart Algo")
 * public class MySmartAlgo implements RobotAlgo {
 *     public Direction next(Tile tile) { ... }
 * }
 * }</pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface VacuumAlgo {
    /** Display name shown in the UI algorithm selector (also used as Spring bean name). */
    @AliasFor(annotation = Component.class)
    String value();
}
