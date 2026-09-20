package com.itvillage.observability;

public final class IncidentObservationEvents {

    public static final String INCIDENT_CREATED = "incident-created";
    public static final String ROUTE_RESOLVED = "route-resolved";
    public static final String KNOWLEDGE_LOADED = "knowledge-loaded";
    public static final String HISTORY_LOADED = "history-loaded";
    public static final String POLICY_RESOLVED = "policy-resolved";
    public static final String ANALYSIS_COMPLETED = "analysis-completed";
    public static final String ACTIONS_RECOMMENDED = "actions-recommended";
    public static final String CRITICAL_ESCALATION_POLICY_RESOLVED = "critical-escalation-policy-resolved";
    public static final String ANALYSIS_PROMPT_BUILT = "analysis-prompt-built";
    public static final String ANALYSIS_AI_RESPONSE_RECEIVED = "analysis-ai-response-received";
    public static final String ANALYSIS_OUTPUT_MAPPING_FAILED = "analysis-output-mapping-failed";
    public static final String ANALYSIS_VALIDATION_FAILED = "analysis-validation-failed";
    public static final String ANALYSIS_RETRY_STARTED = "analysis-retry-started";
    public static final String ANALYSIS_FALLBACK_USED = "analysis-fallback-used";
    public static final String ACTION_PROMPT_BUILT = "action-prompt-built";
    public static final String ACTIONS_AI_RESPONSE_RECEIVED = "actions-ai-response-received";
    public static final String ACTION_VALIDATION_FAILED = "action-validation-failed";
    public static final String ACTION_OUTPUT_MAPPING_FAILED = "action-output-mapping-failed";
    public static final String ACTION_RETRY_STARTED = "action-retry-started";
    public static final String ACTION_FALLBACK_USED = "action-fallback-used";

    private IncidentObservationEvents() {
    }
}
