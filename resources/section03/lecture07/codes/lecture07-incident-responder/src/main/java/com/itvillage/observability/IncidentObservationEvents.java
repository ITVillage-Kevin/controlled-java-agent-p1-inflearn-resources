package com.itvillage.observability;

// 수업 7: 추가
public final class IncidentObservationEvents {

    // IncidentResponderService의 요청 조립 흐름 event
    public static final String INCIDENT_CREATED = "incident-created";

    public static final String KNOWLEDGE_LOADED = "knowledge-loaded";

    public static final String HISTORY_LOADED = "history-loaded";

    public static final String POLICY_RESOLVED = "policy-resolved";

    public static final String ANALYSIS_COMPLETED = "analysis-completed";

    public static final String ACTIONS_RECOMMENDED = "actions-recommended";

    // IncidentAnalysisService 내부 event
    public static final String ANALYSIS_PROMPT_BUILT = "analysis-prompt-built";

    public static final String ANALYSIS_AI_RESPONSE_RECEIVED = "analysis-ai-response-received";

    // IncidentActionRecommendationService 내부 event
    public static final String ACTION_PROMPT_BUILT = "action-prompt-built";

    public static final String ACTIONS_AI_RESPONSE_RECEIVED = "actions-ai-response-received";

    private IncidentObservationEvents() {
    }
}
