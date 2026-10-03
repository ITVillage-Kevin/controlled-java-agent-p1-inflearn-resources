package com.itvillage.incident.port.out;

import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.dto.EscalationDecision;

public interface AlertPolicyPort {
    // 수업 13: 변경
    EscalationDecision resolveEscalationPolicy(String sourceSystem, IncidentSeverity effectiveSeverity);
//    String resolveEscalationPolicy(String sourceSystem, IncidentSeverity severity);
}
