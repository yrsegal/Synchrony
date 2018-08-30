package com.wiresegal.synchrony.service;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.wiresegal.synchrony.authentication.AuthenticationLevel;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A context object that contains all information a service needs, including the session.
 *
 * @author Wire Segal
 */
public final class ServiceContext {
    @NotNull
    private final String excessPath;
    @NotNull
    private final HttpServletRequest request;
    @NotNull
    private final HttpServletResponse response;

    @NotNull
    private final Map<String, String> parameterValues;
    @NotNull
    private final String body;
    @NotNull
    private final HttpSession session;

    /**
     * @param service       The service handling the request.
     * @param excessPath    The excess of the path originally requested.
     * @param request       The request which holds the input data.
     * @param response      The channel that allows us to respond to the request.
     * @throws IOException              If the request was malformed.
     * @throws IllegalArgumentException If required parameters are missing.
     */
    public ServiceContext(WebService service, @NotNull String excessPath, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
            throws IOException, IllegalArgumentException {
        this.excessPath = excessPath;
        this.request = request;
        this.response = response;

        Map<String, String> parameters = service.parseParameters(request);
        if (parameters == null)
            throw new IllegalArgumentException("Invalid parameters for request string" + request.getParameterMap());
        this.parameterValues = parameters;
        this.body = IOUtils.toString(request.getInputStream(), Charset.defaultCharset());
        this.session = request.getSession();
    }

    /**
     * Mark this response as sending data of the given type.
     *
     * @param mime The MIME type to use.
     */
    public void markAs(String mime) {
        response.addHeader("Content-Type", mime);
    }

    /**
     * Send a json object as a response. This also marks the response as json data.
     *
     * @param json The object to send.
     */
    public void send(JsonElement json) {
        try {
            markAs("application/json");
            JsonWriter writer = new JsonWriter(response.getWriter());
            writer.setLenient(true);
            Streams.write(json, writer);
        } catch (IOException ignored) {
            error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send a stream's content as a response.
     *
     * @param stream The InputStream to read.
     */
    public void send(InputStream stream) {
        try {
            IOUtils.copy(stream, response.getWriter(), response.getCharacterEncoding());
        } catch (IOException ignored) {
            error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send a byte array as a response.
     *
     * @param bytes The byte[] to read.
     */
    public void send(byte[] bytes) {
        try {
            IOUtils.write(bytes, response.getWriter(), response.getCharacterEncoding());
        } catch (IOException ignored) {
            error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send a string as a response.
     *
     * @param string The String to read.
     */
    public void send(String string) {
        try {
            IOUtils.write(string, response.getWriter());
        } catch (IOException ignored) {
            error(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Send a resource as a response.
     *
     * @param resource The loaded resource.
     */
    public void send(URL resource) {
        send(resource, UnaryOperator.identity());
    }

    /**
     * Send a resource as a response.
     *
     * @param resource The loaded resource.
     * @param mapper The mapper to transform the resource after loaded.
     */
    public void send(URL resource, UnaryOperator<String> mapper) {
        try {
            if (resource == null)
                error(HttpServletResponse.SC_NOT_FOUND);
            else {
                String read = IOUtils.toString(resource, Charset.defaultCharset());
                send(mapper.apply(read));
            }
        } catch (IOException ignored) {
            error(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    /**
     * Send a resource as a response.
     *
     * @param resource The resource to load.
     * @param clazz The classloader to load from.
     * @param mapper The mapper to transform the resource after loaded.
     */
    public void send(String resource, Class<?> clazz, UnaryOperator<String> mapper) {
        send(resource(resource, clazz), mapper);
    }

    /**
     * Send a resource directly as a response.
     *
     * @param resource The resource to load.
     * @param ctx The context to load from.
     */
    public void send(String resource, ServletContext ctx) {
        send(resource, ctx, UnaryOperator.identity());
    }

    /**
     * Send a resource as a response.
     *
     * @param resource The resource to load.
     * @param ctx The context to load from.
     * @param mapper The mapper to transform the resource after loaded.
     */
    public void send(String resource, ServletContext ctx, UnaryOperator<String> mapper) {
        send(resource(resource, ctx), mapper);
    }

    /**
     * Send a resource directly as a response.
     *
     * @param resource The resource to load.
     * @param clazz The classloader to load from.
     */
    public void send(String resource, Class<?> clazz) {
        send(resource, clazz, UnaryOperator.identity());
    }

    public URL resource(String resource, ServletContext ctx) {
        try {
            return ctx.getResource(resource);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public URL resource(String resource, Class clazz) {
        return clazz.getResource(resource);
    }

    /**
     * @param errorCode The error code to send back.
     */
    public void error(int errorCode) {
        try {
            response.sendError(errorCode);
        } catch (IOException ignored) {
            // NO-OP
        }
    }

    /**
     * @return The excess of the path originally requested.
     */
    @NotNull
    public String getExcessPath() {
        return excessPath;
    }

    /**
     * @return The request to respond to.
     */
    @NotNull
    public HttpServletRequest getRequest() {
        return request;
    }

    /**
     * @return The channel to respond along.
     */
    @NotNull
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @param key The parameter key to get
     * @return The parameter value. May be null.
     */
    @Nullable
    public String getParameter(@NotNull String key) {
        return parameterValues.get(key);
    }

    /**
     * @param key The parameter key to get
     * @return The parameter value, or -1 if null.
     */
    public int getIntParameter(@NotNull String key) {
        String data = getParameter(key);
        if (data == null)
            return -1;

        try {
            return Integer.parseInt(data);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @param key The parameter key to get
     * @return The parameter value, or -1 if null.
     */
    public long getLongParameter(@NotNull String key) {
        String data = getParameter(key);
        if (data == null)
            return -1;

        try {
            return Long.parseLong(data);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * @param key The parameter key to get
     * @return Whether the parameter exists.
     */
    public boolean getBooleanParameter(@NotNull String key) {
        return getParameter(key) != null;
    }

    /**
     * @return The body content of the request.
     */
    @NotNull
    public String getBody() {
        return body;
    }

    /**
     * @return The user's current session. Handled by browsers and servers automatically.
     */
    @NotNull
    public HttpSession getSession() {
        return session;
    }

    @NotNull
    public ServletContext getContext() {
        return request.getServletContext();
    }

    /**
     * @param level The level the user needs.
     * @return Whether the user has that level or not.
     */
    public boolean authenticated(AuthenticationLevel level) {
        Object l = session.getAttribute("auth");
        AuthenticationLevel allowedLevel = l instanceof AuthenticationLevel ? (AuthenticationLevel) l : AuthenticationLevel.NONE;
        return allowedLevel.compareTo(level) >= 0;
    }
}
