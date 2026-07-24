package com.itvillage.incident.adapter.out.ai;

import java.util.List;

public record IncidentAiActionOutput(List<ActionItem> recommendedActions,
                                     String responseNote) {
    public record ActionItem(
            int priority,
            String actionTitle,
            String rationale,
            String ownerSuggestion) {
    }
}
