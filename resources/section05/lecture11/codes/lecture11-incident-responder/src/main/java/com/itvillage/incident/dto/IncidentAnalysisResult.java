package com.itvillage.incident.dto;

import java.util.List;

public record IncidentAnalysisResult(String situationSummary,
                                     List<String> likelyCauses,
                                     String impactAssessment,
                                     String confidenceNote) {
}
