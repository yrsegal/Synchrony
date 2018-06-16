package com.wiresegal.synchrony.service.builders;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wiresegal.synchrony.application.SynchronyApplication;
import com.wiresegal.synchrony.service.ServiceContext;
import com.wiresegal.synchrony.service.WebService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Connection.Method;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author Wire Segal
 * <p>
 * An abstract superclass for service builders, such as views or actions.
 */
public abstract class ServiceBuilder {
    @NotNull
    private final Method method;
    @NotNull
    private final String path;
    @NotNull
    private final String name;
    @NotNull
    private final Set<String> parameters = Sets.newHashSet();
    @NotNull
    private final Map<String, String> optional = Maps.newHashMap();
    @Nullable
    private String description = null;
    private boolean varies = false;
    @Nullable
    private Consumer<ServiceContext> action = null;

    /**
     * @param method The method this service will receive on.
     * @param path   The path this service will receive on. Do not prefix or suffix with slashes.
     * @param name   The name of the service, for OPTIONS documentation.
     */
    public ServiceBuilder(@NotNull Method method, @NotNull String path, @NotNull String name) {
        this.method = method;
        this.path = path;
        this.name = name;
    }

    /**
     * Define a required parameter for this service.
     *
     * @param key The parameter key.
     * @return The builder.
     */
    @NotNull
    public ServiceBuilder parameter(@NotNull String key) {
        parameters.add(key);
        return this;
    }

    /**
     * Define an optional parameter for this service.
     *
     * @param key          The parameter key.
     * @param defaultValue The value to use if the parameter is not present. May be null.
     * @return The builder.
     */
    @NotNull
    public ServiceBuilder parameter(@NotNull String key, @Nullable String defaultValue) {
        optional.put(key, defaultValue);
        return this;
    }

    /**
     * Add an action to this service.
     *
     * @param action The action to take
     * @return The builder.
     */
    @NotNull
    public ServiceBuilder action(@NotNull Consumer<ServiceContext> action) {
        if (this.action == null)
            this.action = action;
        else
            this.action = this.action.andThen(action);

        return this;
    }

    /**
     * Describes this service for OPTIONS documentation.
     *
     * @param description The description string.
     * @return The builder.
     */
    @NotNull
    public ServiceBuilder description(@NotNull String description) {
        this.description = description;
        return this;
    }

    /**
     * Describes the result of this service as nondeterministic.
     * (i.e. separate calls of the same link can give a different result)
     *
     * @return The builder.
     */
    @NotNull
    public ServiceBuilder resultVaries() {
        if (!this.varies) {
            Consumer<ServiceContext> sayVaries = (ctx) -> ctx.getResponse().setHeader("Cache-Control", "private");
            if (this.action == null)
                this.action = sayVaries;
            else
                this.action = sayVaries.andThen(this.action);
        }

        this.varies = true;
        return this;
    }

    /**
     * Creates a service from the provided data.
     *
     * @return The created service.
     */
    @NotNull
    protected abstract WebService build();

    /**
     * Creates an OPTIONS service from the provided data.
     *
     * @return The created service.
     */
    @NotNull
    protected WebService options() {
        return new DocumentationService();
    }

    /**
     * Creates a service, a documentation, and registers them both with your application.
     *
     * @param application The application to register with.
     */
    public void build(SynchronyApplication application) {
        application.addService(method, path, build());
        application.addService(Method.OPTIONS, path, options());
    }

    /**
     * An inner class which provides the default implementation for OPTIONS documentation.
     */
    private class DocumentationService implements WebService {
        private final JsonObject documentation = new JsonObject();

        /**
         * Creates a new documentation service object from the known information.
         */
        public DocumentationService() {
            documentation.addProperty("name", ServiceBuilder.this.name);
            if (ServiceBuilder.this.description != null)
                documentation.addProperty("description", ServiceBuilder.this.description);
            JsonArray requiredParameters = new JsonArray();
            JsonObject optionalParameters = new JsonObject();

            for (String parameter : ServiceBuilder.this.parameters)
                requiredParameters.add(parameter);

            for (Map.Entry<String, String> entry : ServiceBuilder.this.optional.entrySet())
                optionalParameters.addProperty(entry.getKey(), entry.getValue());

            documentation.add("required_parameters", requiredParameters);
            documentation.add("optional_parameters", optionalParameters);
        }

        @Override
        public @NotNull Collection<String> requiredParameters() {
            return Collections.emptyList();
        }

        @Override
        public @NotNull Map<String, String> optionalParameters() {
            return Collections.emptyMap();
        }

        @Override
        public void performAction(ServiceContext context) throws IOException {
            context.send(documentation);
        }
    }
}
