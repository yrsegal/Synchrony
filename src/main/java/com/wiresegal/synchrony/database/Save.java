package com.wiresegal.synchrony.database;

import java.lang.annotation.*;

/**
 * @author Wire Segal
 *
 * The repeatable meta-magic annotation for serialization.
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
