package com.itvillage.incident.adapter.out.ai;

import java.util.List;

public record IncidentAiAnalysisOutput(String situationSummary,
                                       List<String> likelyCauses,
                                       String impactAssessment,
                                       String confidenceNote) {
}
