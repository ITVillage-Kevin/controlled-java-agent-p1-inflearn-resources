package com.itvillage.incident.application.cost;

// 수업 10: 추가
public record IncidentPromptBudget(int maxSourceSystemCharacters,
                                   int maxTitleCharacters,
                                   int maxSummaryCharacters,
                                   int maxSymptomsCharacters,

                                   int maxSituationSummaryCharacters,
                                   int maxLikelyCausesCharacters,
                                   int maxImpactAssessmentCharacters,
                                   int maxConfidenceNoteCharacters,

                                   int maxAnalysisPromptCharacters,
                                   int maxActionPromptCharacters) {
    public static IncidentPromptBudget defaultBudget() {
        return new IncidentPromptBudget(
                120,
                240,
                1_200,
                1_000,

                800,
                900,
                600,
                400,

                12_000,
                8_000);
    }
}
