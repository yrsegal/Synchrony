package com.wiresegal.synchrony.database;

import java.lang.annotation.*;

/**
 * The repeatable meta-magic annotation for serialization.
 *
 * @author Wire Segal
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(SaveTo.class)
public @interface Save {
    /**
     * @return The serialization target to aim for.
     */
    String target() default "";
}
