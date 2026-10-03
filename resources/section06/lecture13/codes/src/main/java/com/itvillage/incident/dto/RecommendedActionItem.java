package com.itvillage.incident.dto;

public record RecommendedActionItem(int priority,
                                    String actionTitle,
                                    String rationale,
                                    String ownerSuggestion,
                                    String riskLevel,           // 수업 13: 추가
                                    boolean approvalRequired,   // 수업 13: 추가
                                    String riskReason) {        // 수업 13: 추가
    // 수업 13: 추가
    public RecommendedActionItem(int priority, String actionTitle,
                                 String rationale, String ownerSuggestion) {
        this(priority, actionTitle, rationale, ownerSuggestion,
                "UNCLASSIFIED", false, "Risk classification has not been applied.");
    }
}
