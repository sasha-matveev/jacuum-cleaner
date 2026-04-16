package com.jacuum.algo;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

/**
 * Marks a {@link RobotAlgo} implementation as auto-discoverable.
 * The {@code value} is the display name shown in the UI.
 * If omitted, the simple class name is used.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RobotAlgorithm {
    String value() default "";
}
