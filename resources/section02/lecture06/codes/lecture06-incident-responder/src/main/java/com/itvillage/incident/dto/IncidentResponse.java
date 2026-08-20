package com.itvillage.incident.dto;

import java.util.List;

public record IncidentResponse(String incidentId,
                               String severity,
                               IncidentAnalysisResult analysis,
                               List<RecommendedActionItem> recommendedActions,
                               List<SimilarIncidentSummary> similarIncidents,
                               String escalationPolicy,
                               String responseNote) {
}
