package com.itvillage.observability;

public final class IncidentObservationEvents {

    public static final String INCIDENT_CREATED = "incident-created";

    // 수업 9: 추가
    public static final String ROUTE_RESOLVED = "route-resolved";

    public static final String KNOWLEDGE_LOADED = "knowledge-loaded";

    public static final String HISTORY_LOADED = "history-loaded";

    public static final String POLICY_RESOLVED = "policy-resolved";

    public static final String ANALYSIS_COMPLETED = "analysis-completed";

    public static final String ACTIONS_RECOMMENDED = "actions-recommended";

    // 수업 9: 추가
    public static final String CRITICAL_ESCALATION_POLICY_RESOLVED = "critical-escalation-policy-resolved";

    public static final String ANALYSIS_PROMPT_BUILT = "analysis-prompt-built";

    public static final String ANALYSIS_AI_RESPONSE_RECEIVED = "analysis-ai-response-received";

    public static final String ACTION_PROMPT_BUILT = "action-prompt-built";

    public static final String ACTIONS_AI_RESPONSE_RECEIVED = "actions-ai-response-received";

    private IncidentObservationEvents() {
    }
}
