package com.itvillage.incident.application.quality;

import java.util.List;

// 수업 12: 추가
public record IncidentQualityEvaluation(IncidentQualityDecision decision,
                                        List<String> issues,
                                        List<String> revisionInstructions) {
    public boolean passed() {
        return decision == IncidentQualityDecision.PASS;
    }
}
