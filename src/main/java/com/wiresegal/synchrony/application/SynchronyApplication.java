package com.wiresegal.synchrony.application;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.wiresegal.synchrony.authentication.AuthenticationLevel;
import com.wiresegal.synchrony.authentication.Authenticator;
import com.wiresegal.synchrony.database.Database;
import com.wiresegal.synchrony.database.DatabaseObject;
import com.wiresegal.synchrony.database.DatabaseTypes;
import com.wiresegal.synchrony.service.ServiceContext;
import com.wiresegal.synchrony.service.WebService;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Connection;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.wiresegal.synchrony.authentication.AuthenticationLevel.NONE;

/**
 * The base class for a Synchrony servlet. Create services and data fields in init().
 *
 * @author Wire Segal
 */
public abstract class SynchronyApplication extends HttpServlet {

    private final Map<String, Set<Connection.Method>> options = Maps.newHashMap();
    private final Map<Connection.Method, Map<String, WebService>> services = Maps.newHashMap();

    private final Map<Class<? extends DatabaseObject>, Database> databases = Maps.newHashMap();

    /**
     * Set up your Services and Data Fields here.
     *
     * @throws ServletException For generic servlet errors.
     */
    @Override
    public abstract void init() throws ServletException;

    // ==== Boilerplate

    /**
     * Register a database for a given database type.
     * Loading from stored Synchrony data will be handled if this is called in init.
     *
     * @param clazz    The class of the database information.
     * @param database The new database we are trying to register.
     * @param <T>      The database object type.
     * @return The existing database, or the new database, if none was present.
     */
    @NotNull
    @SuppressWarnings("unchecked")
    public <T extends DatabaseObject> Database<T> database(@NotNull Class<T> clazz, @NotNull Database<T> database) {
        return (Database<T>) databases.computeIfAbsent(clazz, cs -> database);
    }

    /**
     * Register and get a default database for a given database type.
     * Loading from stored Synchrony data will be handled if this is called in init.
     *
     * @param clazz The class of the database information.
     * @param <T>   The database object type.
     * @return The existing database, or the new database, if none was present.
     */
    @NotNull
    public <T extends DatabaseObject> Database<T> database(@NotNull Class<T> clazz) {
        return database(clazz, new Database<>());
    }

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

    // ==== Internals


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            for (Map.Entry<Class<? extends DatabaseObject>, Database> entry : databases.entrySet()) {
                Database<?> db = DatabaseTypes.readDatabase(entry.getKey(), config.getServletContext());
                if (db != null)
                    entry.setValue(db);
            }
        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        try {
            for (Map.Entry<Class<? extends DatabaseObject>, Database> entry : databases.entrySet())
                DatabaseTypes.writeDatabase(entry.getKey(), entry.getValue(), getServletContext());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private static final Pattern AUTHORIZATION = Pattern.compile("Basic ([\\w+/]+={0,2})");

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

        String authorizationHeader = req.getHeader("Authorization");
        if (authorizationHeader != null) {
            Matcher result = AUTHORIZATION.matcher(authorizationHeader);
            if (result.matches()) {
                Database<Authenticator> authenticators = database(Authenticator.class);
                String b64 = result.group(1);
                String[] userData = new String(Base64.getDecoder().decode(b64)).split(":");
                if (userData.length == 2) {
                    String email = userData[0];
                    String pass = userData[1];

                    HttpSession session = req.getSession();
                    Object l = session.getAttribute("auth");
                    AuthenticationLevel level = l instanceof AuthenticationLevel ? (AuthenticationLevel) l : NONE;

                    Optional<AuthenticationLevel> auth = authenticators.stream()
                            .map(authenticator -> authenticator.authenticate(email, pass))
                            .filter(authLevel -> authLevel.compareTo(NONE) > 0)
                            .findFirst();
                    if (auth.isPresent()) {
                        if (auth.get().compareTo(level) > 0)
                            session.setAttribute("auth", auth.get());
                    } else {
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
            }
        }

        String requestKey = req.getRequestURI();
        if (requestKey.startsWith("/"))
            requestKey = requestKey.substring(1);
        if (requestKey.endsWith("/"))
            requestKey = requestKey.substring(0, requestKey.length() - 1);

        String excessPath = "";
        String trueRequest = requestKey;
        while (!trueRequest.isEmpty() && !options.containsKey(trueRequest)) {
            int pos = trueRequest.lastIndexOf('/');
            if (pos > 0) {
                trueRequest = trueRequest.substring(0, pos);
                excessPath = trueRequest.substring(pos) + excessPath;
            } else {
                resp.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
        }

        Set<String> options = getOptions(trueRequest);
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

        WebService handler = handlers.get(trueRequest);
        if (handler == null) {
            resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }

        try {
            ServiceContext context = new ServiceContext(handler, excessPath, req, resp);
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
