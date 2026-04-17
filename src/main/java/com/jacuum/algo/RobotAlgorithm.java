package com.jacuum.algo;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link RobotAlgo} implementation as auto-discoverable.
 * The {@code value} is the display name shown in the UI.
 * If omitted, the simple class name is used.
 */
@Component
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface RobotAlgorithm {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
