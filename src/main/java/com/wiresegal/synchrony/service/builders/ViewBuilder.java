package com.wiresegal.synchrony.service.builders;

import com.wiresegal.synchrony.database.Database;
import com.wiresegal.synchrony.database.DatabaseObject;
import com.wiresegal.synchrony.database.DatabaseTypes;
import com.wiresegal.synchrony.service.ServiceContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Connection;

import javax.servlet.http.HttpServletResponse;
import java.util.function.Consumer;

/**
 * A builder class for creating a "View" on a database.
 *
 * @param <E> The type the database holds.
 * @author Wire Segal
 */
public class ViewBuilder<E extends DatabaseObject> extends ServiceBuilder {

    @NotNull
    private final Database<E> database;
    @NotNull
    private String target = "";

    /**
     * Construct a new builder for a database view.
     * This defines the parameter "id" and the response action.
     *
     * @param database The database to look up from.
     * @param path     The path this service will receive on. Do not prefix or suffix with slashes.
     * @param name     The name of the service, for OPTIONS documentation.
     */
    public ViewBuilder(@NotNull Database<E> database, @NotNull String path, @NotNull String name) {
        super(Connection.Method.GET, path, name);
        this.database = database;

        parameter("id").action(this::sendDatabaseValue);
    }

    /**
     * Define the serialization target for this builder.
     *
     * @param target The desired target. An empty string is the default target.
     * @return The builder.
     */
    @NotNull
    public ViewBuilder<E> target(@NotNull String target) {
        this.target = target;
        return this;
    }

    @Override
    @NotNull
    public ViewBuilder<E> parameter(@NotNull String key) {
        super.parameter(key);
        return this;
    }

    @Override
    @NotNull
    public ViewBuilder<E> parameter(@NotNull String key, @Nullable String defaultValue) {
        super.parameter(key, defaultValue);
        return this;
    }

    @Override
    @NotNull
    public ViewBuilder<E> action(@NotNull Consumer<ServiceContext> action) {
        super.action(action);
        return this;
    }

    @Override
    @NotNull
    public ViewBuilder<E> description(@NotNull String description) {
        super.description(description);
        return this;
    }

    @Override
    @NotNull
    public ViewBuilder<E> resultVaries() {
        super.resultVaries();
        return this;
    }

    /**
     * Serve this database value to the client.
     *
     * @param context The context, containing an 'id' parameter.
     */
    private void sendDatabaseValue(ServiceContext context) {
        long id = context.getLongParameter("id");
        E target = database.lookup(id);
        if (target == null)
            context.error(HttpServletResponse.SC_NOT_FOUND);
        else
            context.send(DatabaseTypes.write(target, this.target));
    }


}
