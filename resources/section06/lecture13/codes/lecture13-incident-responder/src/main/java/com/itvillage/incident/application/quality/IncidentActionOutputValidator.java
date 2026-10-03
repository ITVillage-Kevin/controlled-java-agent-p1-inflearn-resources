package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class IncidentActionOutputValidator {

    private static final int REQUIRED_ACTION_COUNT = 3;

    private static final int MIN_PRIORITY = 1;
    private static final int MAX_PRIORITY = 3;

    public ValidationResult validate(IncidentAiActionOutput output) {
        List<String> violations = new ArrayList<>();

        if (output == null) {
            return ValidationResult.failure(List.of("action output must not be null."));
        }

        validateActions(output.recommendedActions(), violations);

        if (isBlank(output.responseNote())) {
            violations.add("responseNote must not be blank.");
        }

        return ValidationResult.from(violations);
    }

    private void validateActions(List<IncidentAiActionOutput.ActionItem> actions, List<String> violations) {
        if (actions == null) {
            violations.add("recommendedActions must not be null.");
            return;
        }

        if (actions.size() != REQUIRED_ACTION_COUNT) {
            violations.add("recommendedActions must contain exactly " + REQUIRED_ACTION_COUNT + " items.");
        }

        Set<Integer> priorities = new HashSet<>();

        Set<String> titles = new HashSet<>();

        for (IncidentAiActionOutput.ActionItem action : actions) {

            if (action == null) {
                violations.add("recommendedActions must not contain null items.");
                continue;
            }

            if (action.priority() < MIN_PRIORITY || action.priority() > MAX_PRIORITY) {
                violations.add("priority must be between " + MIN_PRIORITY + " and " + MAX_PRIORITY + ".");
            }

            if (!priorities.add(action.priority())) {
                violations.add("priority values must not be duplicated.");
            }

            if (isBlank(action.actionTitle())) {
                violations.add("actionTitle must not be blank.");
            } else {
                String normalizedTitle = normalize(action.actionTitle());

                if (!titles.add(normalizedTitle)) {
                    violations.add("actionTitle must not be duplicated.");
                }
            }

            if (isBlank(action.rationale())) {
                violations.add("rationale must not be blank.");
            }

            if (isBlank(action.ownerSuggestion())) {
                violations.add("ownerSuggestion must not be blank.");
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