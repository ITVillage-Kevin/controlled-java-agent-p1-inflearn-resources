package com.itvillage.incident.application.routing;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentResponse;

public interface IncidentRouteHandler {
    IncidentRoute route();
    IncidentResponse handle(Incident incident, RoutingDecision decision);
}
