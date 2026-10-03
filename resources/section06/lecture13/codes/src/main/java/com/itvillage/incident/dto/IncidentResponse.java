package com.itvillage.incident.dto;

import java.util.List;

public record IncidentResponse(String incidentId,
                               String severity,
                               String requestedSeverity, // 수업 13: 추가
                               String route,
                               String routeReason,
                               IncidentAnalysisResult analysis,
                               List<RecommendedActionItem> recommendedActions,
                               List<SimilarIncidentSummary> similarIncidents,
                               EscalationDecision escalationPolicy, // 수업 13: 변경
                               String responseNote) {
}