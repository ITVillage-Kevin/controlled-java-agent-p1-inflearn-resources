package com.itvillage.incident.application.routing;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentResponse;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;

@Component
public class StandardAnalysisIncidentRouteHandler implements IncidentRouteHandler {

    private final IncidentResponseWorkflow workflow;

    public StandardAnalysisIncidentRouteHandler(IncidentResponseWorkflow workflow) {
        this.workflow = workflow;
    }

    @Override
    public IncidentRoute route() {
        return IncidentRoute.STANDARD_ANALYSIS;
    }

    @Override
    @Observed(name = "incident.routing.standard-analysis", contextualName = "route-standard-analysis")
    public IncidentResponse handle(Incident incident, RoutingDecision decision) {
        return workflow.executeStandard(incident, decision);
    }
}
