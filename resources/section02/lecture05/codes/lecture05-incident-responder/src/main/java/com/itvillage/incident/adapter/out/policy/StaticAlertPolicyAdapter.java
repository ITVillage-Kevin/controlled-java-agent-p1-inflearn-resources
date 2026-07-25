package com.itvillage.incident.adapter.out.policy;

import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.port.out.AlertPolicyPort;
import org.springframework.stereotype.Component;

@Component
public class StaticAlertPolicyAdapter implements AlertPolicyPort {

    @Override
    public String resolveEscalationPolicy(String sourceSystem, IncidentSeverity severity) {
        return switch (severity) {
            case LOW -> "Log and monitor. Notify service owner during business hours.";
            case MEDIUM -> "Notify on-call engineer and monitor error/latency trend for 15 minutes.";
            case HIGH -> "Page primary on-call immediately and prepare stakeholder communication.";
            case CRITICAL -> "Page primary and secondary on-call immediately. Escalate to incident commander and open war room.";
        };
    }
}
