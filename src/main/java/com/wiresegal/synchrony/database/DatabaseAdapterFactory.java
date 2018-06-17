package com.wiresegal.synchrony.database;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * An adapter factory for {@link Database} objects.
 *
 * @author Wire Segal
 */
public class DatabaseAdapterFactory implements TypeAdapterFactory {
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> TypeAdapter<T> create(@NotNull Gson gson, @NotNull TypeToken<T> typeToken) {
        Type type = typeToken.getType();
        if (typeToken.getRawType() != Database.class
                || !(type instanceof ParameterizedType)) {
            return null;
        }

        Type elementType = ((ParameterizedType) type).getActualTypeArguments()[0];
        TypeAdapter<?> elementAdapter = gson.getAdapter(TypeToken.get(elementType));
        return (TypeAdapter<T>) newDatabaseAdapter((TypeAdapter<? extends DatabaseObject>) elementAdapter);
    }

    /**
     * Creates a new typed database adapter.
     *
     * @param elementAdapter The adapter for the database object.
     * @param <E>            The type of object being adapted.
     * @return A new type adapter that handles that type of database.
     */
    @NotNull
    private <E extends DatabaseObject> TypeAdapter<Database<E>> newDatabaseAdapter(
            final @NotNull TypeAdapter<E> elementAdapter) {
        return new TypeAdapter<Database<E>>() {
            public void write(JsonWriter out, Database<E> value) throws IOException {
                if (value == null) {
                    out.nullValue();
                    return;
                }

                out.beginObject();
                out.name("allocation").value(value.totalAllocated());
                out.name("values").beginArray();
                for (E db : value) elementAdapter.write(out, db);
                out.endArray();
                out.endObject();
            }

            public Database<E> read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }

                Set<E> values = new HashSet<>();
                long allocation = 0;
                in.beginObject();
                while (in.peek() == JsonToken.NAME) {
                    String key = in.nextName();
                    if ("allocation".equals(key))
                        allocation = in.nextLong();
                    else if ("values".equals(key)) {
                        in.beginArray();
                        while (in.peek() != JsonToken.END_ARRAY)
                            values.add(elementAdapter.read(in));
                        in.endArray();
                    }
                }
                in.endObject();
                return new Database<>(allocation, values);
            }
        };
    }
}
