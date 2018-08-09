package com.wiresegal.synchrony.database;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
    public static JsonElement write(@NotNull Object object, @NotNull String target) {
        return forTarget(target).toJsonTree(object);
    }

    /**
     * @param object The object to serialize.
     * @param target The target to serialize to. Use an empty string for "default."
     * @param stream The stream to write out to.
     */
    public static void write(@NotNull Object object, @NotNull String target, @NotNull OutputStream stream) {
        forTarget(target).toJson(object, new OutputStreamWriter(stream));
    }

    /**
     * @param object The json data to read from.
     * @param target The target to serialize to. Use an empty string for "default".
     * @param type   The type of the object.
     * @param <T>    The type of the object.
     * @return The deserialized object.
     */
    @NotNull
    public static <T> T read(@NotNull JsonElement object, @NotNull String target, @NotNull TypeToken<T> type) {
        return forTarget(target).fromJson(object, type.getType());
    }

    /**
     * @param stream The stream to read json data from.
     * @param target The target to serialize to. Use an empty string for "default".
     * @param type   The type of the object.
     * @param <T>    The type of the object.
     * @return The deserialized object.
     */
    @NotNull
    public static <T> T read(@NotNull InputStream stream, @NotNull String target, @NotNull TypeToken<?> type) {
        return forTarget(target).fromJson(new InputStreamReader(stream), type.getType());
    }

    /**
     * @param clazz    The database type.
     * @param database The database object to write out.
     * @param context  The context of the servlet, used to gain relative paths.
     * @param <T>      The type of the database.
     * @throws IOException If file IO failed.
     */
    public static <T extends DatabaseObject> void writeDatabase(Class<T> clazz, Database<?> database, ServletContext context) throws IOException {
        File location = databaseFile(clazz, context);
        if (location.exists() || location.getParentFile().mkdirs()) {
            FileOutputStream stream = new FileOutputStream(location);
            GZIPOutputStream gz = new GZIPOutputStream(stream);
            write(database, "*", gz);
            gz.close();
            stream.close();
        }
    }

    /**
     * @param clazz   The database type.
     * @param context The context of the servlet, used to gain relative paths.
     * @return The database read out, or null if none could be read.
     * @throws IOException If file IO failed.
     */
    @Nullable
    public static Database<? extends DatabaseObject> readDatabase(Class<? extends DatabaseObject> clazz, ServletContext context) throws IOException {
        File location = databaseFile(clazz, context);
        if (location.exists() || location.getParentFile().mkdirs()) {
            FileInputStream stream = new FileInputStream(location);
            GZIPInputStream gz = new GZIPInputStream(stream);
            Database<? extends DatabaseObject> db = read(gz, "*", TypeToken.getParameterized(Database.class, clazz));
            gz.close();
            stream.close();

            return db;
        }

        return null;
    }

    /**
     * @param clazz   The class getting a database
     * @param context The context of the servlet, used to gain relative paths.
     * @return The location of the database.
     */
    public static File databaseFile(Class<?> clazz, ServletContext context) {
        String name = clazz.getSimpleName().toLowerCase() + ".json.gz";
        String realPath = context.getRealPath("/");
        return new File(realPath, name);
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
                        targets.put(field.getName(), Sets.newHashSet("*"));
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
            return targets.containsKey(str) && (targets.get(str).contains("") || targets.get(str).contains(target));
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
