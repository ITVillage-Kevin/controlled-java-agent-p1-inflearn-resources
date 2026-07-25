package com.itvillage.incident.port.out;

import com.itvillage.incident.domain.IncidentSeverity;

public interface AlertPolicyPort {
    String resolveEscalationPolicy(String sourceSystem, IncidentSeverity severity);
}
