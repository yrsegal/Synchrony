package com.wiresegal.synchrony.database;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * Serialization utilities involving {@link Save}.
 *
 * @author Wire Segal
 */
public final class DatabaseTypes {
    @NotNull
    private static final Map<Class<?>, SerializationData> SERIALIZATION = Maps.newHashMap();
    @NotNull
    private static final Map<String, Gson> SERIALIZERS = Maps.newHashMap();

    /**
     * @param object The object to serialize.
     * @param target The target to serialize to. Use an empty string for "default."
     * @return The serialized json.
     */
    @NotNull
    public static JsonElement write(@NotNull DatabaseObject object, @NotNull String target) {
        return forTarget(target).toJsonTree(object);
    }

    /**
     * @param object The json data to read from.
     * @param target The target to serialize to. Use an empty string for "default".
     * @param clazz  The type of the object.
     * @param <T>    The type of the object.
     * @return The deserialized object.
     */
    @NotNull
    public static <T extends DatabaseObject> T read(@NotNull JsonElement object, @NotNull String target, @NotNull Class<T> clazz) {
        return forTarget(target).fromJson(object, clazz);
    }

    /**
     * @param db The class to get serialization data for.
     * @return The serialization data for the class.
     */
    @NotNull
    private static SerializationData forClass(@NotNull Class<?> db) {
        return SERIALIZATION.computeIfAbsent(db, SerializationData::new);
    }

    /**
     * @param target The target to serialize to.
     * @return A {@link Gson} instance keyed to the target.
     */
    @NotNull
    private static Gson forTarget(String target) {
        return SERIALIZERS.computeIfAbsent(target, t -> new GsonBuilder()
                .setExclusionStrategies(new TargetedExclude(t))
                .registerTypeAdapterFactory(new DatabaseAdapterFactory())
                .create());
    }


    /**
     * Cached data on annotated class fields.
     */
    private static class SerializationData {
        @NotNull
        private final Map<String, Set<String>> targets = Maps.newHashMap();

        /**
         * @param clazz The class to create serialization data from.
         */
        public SerializationData(@NotNull Class<?> clazz) {
            Class<?> target = clazz;
            while (target != null) {
                for (Field field : target.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Save.class)) {
                        targets.put(field.getName(), Sets.newHashSet());
                        for (Save save : field.getAnnotationsByType(Save.class))
                            targets.get(field.getName()).add(save.target());
                    }
                }
                target = target.getSuperclass();
            }
        }

        /**
         * @param str    The field name.
         * @param target The serialization target.
         * @return Whether to serialize that field.
         */
        public boolean applies(@NotNull String str, @NotNull String target) {
            return targets.containsKey(str) && (targets.get("").contains(target) || targets.get(str).contains(target));
        }
    }

    /**
     * An exclusion strategy based on a target name.
     */
    private static class TargetedExclude implements ExclusionStrategy {
        @NotNull
        private final String target;

        /**
         * @param target The target to serialize with.
         */
        public TargetedExclude(@NotNull String target) {
            this.target = target;
        }

        @Override
        public boolean shouldSkipField(@NotNull FieldAttributes f) {
            SerializationData data = forClass(f.getDeclaringClass());
            String name = f.getName();
            return data.applies(name, target);
        }

        @Override
        public boolean shouldSkipClass(@NotNull Class<?> clazz) {
            return false;
        }
    }
}
