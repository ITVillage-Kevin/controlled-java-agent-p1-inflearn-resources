package com.itvillage.incident.application.risk;

import com.itvillage.incident.adapter.out.policy.AlertPolicyCatalog;
import com.itvillage.incident.adapter.out.policy.ServicePolicyDefinition;
import com.itvillage.incident.domain.IncidentSeverity;
import org.springframework.stereotype.Component;

// 수업 13: 추가
@Component
public class IncidentSeverityPolicy {
    private final AlertPolicyCatalog catalog;

    public IncidentSeverityPolicy(AlertPolicyCatalog catalog) {
        this.catalog = catalog;
    }

    public IncidentSeverity effectiveSeverity(String sourceSystem, IncidentSeverity incidentSeverity) {
        if (incidentSeverity == null) {
            throw new IllegalArgumentException("Incident severity is required.");
        }
        ServicePolicyDefinition service = catalog.findServicePolicy(sourceSystem);
        if (service == null || service.minimumSeverity() == null) {
            return incidentSeverity;
        }
        return incidentSeverity.ordinal() >= service.minimumSeverity().ordinal()
                ? incidentSeverity : service.minimumSeverity();
    }
}
