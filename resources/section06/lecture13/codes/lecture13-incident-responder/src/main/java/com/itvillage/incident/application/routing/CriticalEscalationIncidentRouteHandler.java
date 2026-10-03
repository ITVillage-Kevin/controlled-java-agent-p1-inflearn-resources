package com.itvillage.incident.application.routing;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentResponse;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;

@Component
public class CriticalEscalationIncidentRouteHandler implements IncidentRouteHandler {

    private final IncidentResponseWorkflow workflow;

    public CriticalEscalationIncidentRouteHandler(IncidentResponseWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public IncidentRoute route() {
        return IncidentRoute.CRITICAL_ESCALATION;
    }

    @Override
    @Observed(name = "incident.routing.critical-escalation", contextualName = "route-critical-escalation")
    public IncidentResponse handle(Incident incident, RoutingDecision decision) {
        return workflow.executeCritical(incident, decision);
    }
}
