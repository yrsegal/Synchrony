package com.wiresegal.synchrony.service;

import com.google.common.collect.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author Wire Segal
 * <p>
 * A shared interface for all web actions and views. Specifies parameters and the action that can then be taken.
 */
public interface WebService {

    /**
     * @return A collection of parameters that this service requires to be successful.
     */
    @NotNull
    Collection<String> requiredParameters();

    /**
     * @return A set of values this service can take optionally. Keys cannot be null. Values can be null.
     */
    @NotNull
    Map<String, String> optionalParameters();

    /**
     * Invoke the request handler.
     *
     * @param context The context from which the action is being performed.
     * @throws IOException When accessing the request and response fails.
     */
    void performAction(ServiceContext context) throws IOException;


    /**
     * Parses parameters from a request.
     *
     * @param request The incoming request.
     * @return The parameter values found, or null if the request was invalid.
     */
    @Nullable
    default Map<String, String> parseParameters(@NotNull HttpServletRequest request) {
        Map<String, String> parameters = Maps.newHashMap();

        Set<String> parameterNames = request.getParameterMap().keySet();

        for (String parameter : requiredParameters()) {
            if (!parameterNames.contains(parameter))
                return null;
            parameters.put(parameter, request.getParameter(parameter));
        }

        for (Map.Entry<String, String> parameterInfo : optionalParameters().entrySet()) {
            String parameter = parameterInfo.getKey();
            String defaultValue = parameterInfo.getValue();

            if (parameterNames.contains(parameter))
                parameters.put(parameter, request.getParameter(parameter));
            else
                parameters.put(parameter, defaultValue);
        }

        return parameters;
    }
}
