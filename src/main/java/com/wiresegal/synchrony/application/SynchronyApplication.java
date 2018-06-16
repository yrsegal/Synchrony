package com.wiresegal.synchrony.application;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.wiresegal.synchrony.service.ServiceContext;
import com.wiresegal.synchrony.service.WebService;
import org.jsoup.Connection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Wire Segal
 * <p>
 * The base class for a Synchrony servlet. Create services and data fields in init().
 */
public abstract class SynchronyApplication extends HttpServlet {

    private final Map<String, Set<Connection.Method>> options = Maps.newHashMap();
    private final Map<Connection.Method, Map<String, WebService>> services = Maps.newHashMap();

    /**
     * Set up your Services and Data Fields here.
     *
     * @throws ServletException For generic servlet errors.
     */
    @Override
    public abstract void init() throws ServletException;

    // ==== Boilerplate

    // todo data fields

    /**
     * Subscribes a service to the handlers for this servlet. This should not be called directly.
     *
     * @param method  The method (GET, POST, etc.) to listen on.
     * @param path    The path to listen for.
     * @param service The service to execute.
     */
    public void addService(Connection.Method method, String path, WebService service) {
        options.computeIfAbsent(path, (p) -> Sets.newHashSet()).add(method);
        services.computeIfAbsent(method, (m) -> Maps.newHashMap()).put(path, service);
    }

    /**
     * Get the valid response types for the given path.
     *
     * @param path A request path.
     * @return The set of response types. Can be empty.
     */
    public Set<String> getOptions(String path) {
        return options.getOrDefault(path, Collections.emptySet()).stream()
                .map(Connection.Method::name)
                .collect(Collectors.toSet());
    }

    /**
     * Executes a service from request data.
     *
     * @param method The method (GET, POST, etc.) used for the request.
     * @param req    The request object.
     * @param resp   The response object.
     * @throws IOException If the handler could not be written to.
     */
    private void executeService(Connection.Method method, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        String requestKey = req.getHttpServletMapping().getMatchValue();
        if (requestKey.startsWith("/"))
            requestKey = requestKey.substring(1);
        if (requestKey.endsWith("/"))
            requestKey = requestKey.substring(0, requestKey.length() - 1);

        Set<String> options = getOptions(requestKey);
        if (options.isEmpty()) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setHeader("Allow", String.join(", ", options));

        Map<String, WebService> handlers = services.get(method);
        if (handlers == null) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        WebService handler = handlers.get(requestKey);
        if (handler == null) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try {
            ServiceContext context = new ServiceContext(handler, req, resp);
            handler.performAction(context);
        } catch (IllegalArgumentException e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.GET, req, resp);
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.HEAD, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.POST, req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.PUT, req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.DELETE, req, resp);
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.OPTIONS, req, resp);
    }

    @Override
    protected void doTrace(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        executeService(Connection.Method.TRACE, req, resp);
    }
}
