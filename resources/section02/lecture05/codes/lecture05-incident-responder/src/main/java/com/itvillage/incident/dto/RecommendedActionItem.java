package com.itvillage.incident.dto;

public record RecommendedActionItem(int priority,
                                    String actionTitle,
                                    String rationale,
                                    String ownerSuggestion) {
}
