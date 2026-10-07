package com.itvillage.incident.application.routing;

import org.springframework.stereotype.Component;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class IncidentRouteHandlerRegistry {

    private final Map<IncidentRoute, IncidentRouteHandler> handlers;

    public IncidentRouteHandlerRegistry(List<IncidentRouteHandler> handlers) {
        Map<IncidentRoute, IncidentRouteHandler> mappedHandlers = new EnumMap<>(IncidentRoute.class);

        for (IncidentRouteHandler handler : handlers) {
            IncidentRoute route = handler.route();

            IncidentRouteHandler previous = mappedHandlers.put(route, handler);

            if (previous != null) {
                throw new IllegalStateException("Duplicate route handler registered for route: " + route);
            }
        }

        this.handlers = Map.copyOf(mappedHandlers);
    }

    public IncidentRouteHandler getRequiredHandler(IncidentRoute route) {
        IncidentRouteHandler handler = handlers.get(route);

        if (handler == null) {
            throw new IllegalStateException("No route handler registered for route: " + route);
        }

        return handler;
    }
}
