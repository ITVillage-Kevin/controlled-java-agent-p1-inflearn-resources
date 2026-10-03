package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class IncidentAnalysisOutputValidator {

    private static final int MAX_LIKELY_CAUSE_COUNT = 5;

    public ValidationResult validate(IncidentAiAnalysisOutput output) {
        List<String> violations = new ArrayList<>();

        if (output == null) {
            return ValidationResult.failure(List.of("analysis output must not be null."));
        }

        if (isBlank(output.situationSummary())) {
            violations.add("situationSummary must not be blank.");
        }

        validateLikelyCauses(output.likelyCauses(), violations);

        if (isBlank(output.impactAssessment())) {
            violations.add("impactAssessment must not be blank.");
        }

        if (isBlank(output.confidenceNote())) {
            violations.add("confidenceNote must not be blank.");
        }

        return ValidationResult.from(violations);
    }

    private void validateLikelyCauses(List<String> likelyCauses, List<String> violations) {
        if (likelyCauses == null || likelyCauses.isEmpty()) {
            violations.add("likelyCauses must contain at least one item.");
            return;
        }

        if (likelyCauses.size() > MAX_LIKELY_CAUSE_COUNT) {
            violations.add("likelyCauses must contain at most " + MAX_LIKELY_CAUSE_COUNT + " items.");
        }

        Set<String> normalized = new HashSet<>();

        for (String cause : likelyCauses) {
            if (isBlank(cause)) {
                violations.add("likelyCauses must not contain blank items.");
                continue;
            }

            String normalizedCause = normalize(cause);

            if (!normalized.add(normalizedCause)) {
                violations.add("likelyCauses must not contain duplicate items.");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
