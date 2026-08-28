package com.itvillage.incident.adapter.out.policy;

import com.itvillage.incident.domain.IncidentSeverity;

public record ServicePolicyDefinition(String businessCriticality,
                                      IncidentSeverity minimumSeverity) {
}
