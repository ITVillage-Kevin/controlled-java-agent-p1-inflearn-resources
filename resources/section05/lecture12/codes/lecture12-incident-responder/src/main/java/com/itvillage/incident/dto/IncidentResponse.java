package com.itvillage.incident.dto;

import java.util.List;

public record IncidentResponse(String incidentId,
                               String severity,
                               String route, // 수업 9: 추가
                               String routeReason, // 수업 9: 추가
                               IncidentAnalysisResult analysis,
                               List<RecommendedActionItem> recommendedActions,
                               List<SimilarIncidentSummary> similarIncidents,
                               String escalationPolicy,
                               String responseNote) {
}