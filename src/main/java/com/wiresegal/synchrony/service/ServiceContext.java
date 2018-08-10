package com.wiresegal.synchrony.service;

import com.google.gson.JsonElement;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.wiresegal.synchrony.authentication.AuthenticationLevel;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * A context object that contains all information a service needs, including the session.
 *
 * @author Wire Segal
 */
public final class ServiceContext {
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
     * @param service  The service handling the request.
     * @param request  The request which holds the input data.
     * @param response The channel that allows us to respond to the request.
     * @throws IOException              If the request was malformed.
     * @throws IllegalArgumentException If required parameters are missing.
     */
    public ServiceContext(WebService service, @NotNull HttpServletRequest request, @NotNull HttpServletResponse response)
            throws IOException, IllegalArgumentException {
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
            // NO-OP
        }
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
     * @param level The level the user needs.
     * @return Whether the user has that level or not.
     */
    public boolean authenticated(AuthenticationLevel level) {
        Object l = session.getAttribute("auth");
        AuthenticationLevel allowedLevel = l instanceof AuthenticationLevel ? (AuthenticationLevel) l : AuthenticationLevel.NONE;
        return allowedLevel.compareTo(level) >= 0;
    }
}
