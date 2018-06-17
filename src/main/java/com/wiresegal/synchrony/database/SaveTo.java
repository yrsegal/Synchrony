package com.wiresegal.synchrony.database;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The container annotation for Save.
 *
 * @author Wire Segal
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface SaveTo {
    /**
     * @return The Save targets to hit.
     */
    @SuppressWarnings("unused")
    Save[] value();
}
