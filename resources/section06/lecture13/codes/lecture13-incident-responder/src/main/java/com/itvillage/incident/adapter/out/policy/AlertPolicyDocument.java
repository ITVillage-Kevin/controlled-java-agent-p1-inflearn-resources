package com.itvillage.incident.adapter.out.policy;

import com.itvillage.incident.domain.IncidentSeverity;

import java.util.Map;

public record AlertPolicyDocument(Map<String, ServicePolicyDefinition> services,
                                  Map<IncidentSeverity, SeverityPolicyDefinition> severityPolicies) {
}
