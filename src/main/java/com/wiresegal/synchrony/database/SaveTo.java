package com.wiresegal.synchrony.database;

import java.lang.annotation.*;

/**
 * @author Wire Segal
 *
 * The container annotation for Save.
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
