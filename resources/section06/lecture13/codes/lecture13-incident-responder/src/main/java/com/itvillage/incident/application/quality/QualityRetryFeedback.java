package com.itvillage.incident.application.quality;

import java.util.List;
import java.util.stream.Collectors;

public record QualityRetryFeedback(String outputType,
                                   List<String> violations) {
    public String toPromptText() {
        String violationText =
                violations.stream()
                        .map(violation -> "- " + violation)
                        .collect(Collectors.joining(System.lineSeparator()));

        return """
                CORRECTION RETRY

                Your previous %s response did not satisfy
                the required output contract.

                Violations:
                %s

                Correct every violation listed above.

                Return JSON only.
                Do not include markdown fences.
                Do not add explanations before or after JSON.
                """.formatted(outputType, violationText);
    }
}
