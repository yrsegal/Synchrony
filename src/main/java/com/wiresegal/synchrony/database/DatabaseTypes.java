package com.wiresegal.synchrony.database;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

/**
 * @author Wire Segal
 *
 * Serialization utilities involving {@link Save}.
 */
public final class DatabaseTypes {
    private static final Map<Class<?>, SerializationData> SERIALIZATION = Maps.newHashMap();
    private static final Map<String, Gson> SERIALIZERS = Maps.newHashMap();

    /**
     * @param object The object to serialize.
     * @param target The target to serialize to. Use an empty string for "default."
     * @return The serialized json.
     */
    public static JsonElement write(DatabaseObject object, String target) {
        return forTarget(target).toJsonTree(object);
    }

    /**
     * @param object The json data to read from.
     * @param target The target to serialize to. Use an empty string for "default".
     * @param clazz The type of the object.
     * @param <T> The type of the object.
     * @return The deserialized object.
     */
    public static <T extends DatabaseObject> T read(JsonElement object, String target, Class<T> clazz) {
        return forTarget(target).fromJson(object, clazz);
    }

    /**
     * @param db The class to get serialization data for.
     * @return The serialization data for the class.
     */
    private static SerializationData forClass(Class<?> db) {
        return SERIALIZATION.computeIfAbsent(db, SerializationData::new);
    }

    /**
     * @param target The target to serialize to.
     * @return A {@link Gson} instance keyed to the target.
     */
    private static Gson forTarget(String target) {
        return SERIALIZERS.computeIfAbsent(target, t -> new GsonBuilder()
                .setExclusionStrategies(new TargetedExclude(t)).create());
    }


    /**
     * Cached data on annotated class fields.
     */
    private static class SerializationData {
        private final Map<String, Set<String>> targets = Maps.newHashMap();

        /**
         * @param clazz The class to create serialization data from.
         */
        public SerializationData(Class<?> clazz) {
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
         * @param str The field name.
         * @param target The serialization target.
         * @return Whether to serialize that field.
         */
        public boolean applies(String str, String target) {
            return targets.containsKey(str) && (targets.get("").contains(target) || targets.get(str).contains(target));
        }
    }

    /**
     * An exclusion strategy based on a target name.
     */
    private static class TargetedExclude implements ExclusionStrategy {
        private final String target;

        /**
         * @param target The target to serialize with.
         */
        public TargetedExclude(String target) {
            this.target = target;
        }

        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            SerializationData data = forClass(f.getDeclaringClass());
            String name = f.getName();
            return data.applies(name, target);
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
