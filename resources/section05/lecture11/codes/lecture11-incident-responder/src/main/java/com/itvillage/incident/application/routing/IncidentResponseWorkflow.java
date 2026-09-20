package com.itvillage.incident.application.routing;

import com.itvillage.incident.application.IncidentActionRecommendationService;
import com.itvillage.incident.application.IncidentAnalysisService;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentResponse;
import com.itvillage.incident.dto.RecommendedActionItem;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import com.itvillage.incident.port.out.AlertPolicyPort;
import com.itvillage.incident.port.out.IncidentHistoryPort;
import com.itvillage.incident.port.out.IncidentKnowledgePort;
import com.itvillage.observability.AiObservationSupport;
import com.itvillage.observability.IncidentObservationEvents;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.itvillage.observability.IncidentObservationEvents.*;

@Component
public class IncidentResponseWorkflow {

    private final IncidentKnowledgePort incidentKnowledgePort;

    private final IncidentHistoryPort incidentHistoryPort;

    private final AlertPolicyPort alertPolicyPort;

    private final IncidentAnalysisService incidentAnalysisService;

    private final IncidentActionRecommendationService incidentActionRecommendationService;

    private final AiObservationSupport observationSupport;

    public IncidentResponseWorkflow(IncidentKnowledgePort incidentKnowledgePort,
                                    IncidentHistoryPort incidentHistoryPort,
                                    AlertPolicyPort alertPolicyPort,
                                    IncidentAnalysisService incidentAnalysisService,
                                    IncidentActionRecommendationService incidentActionRecommendationService,
                                    AiObservationSupport observationSupport) {
        this.incidentKnowledgePort = incidentKnowledgePort;
        this.incidentHistoryPort = incidentHistoryPort;
        this.alertPolicyPort = alertPolicyPort;
        this.incidentAnalysisService = incidentAnalysisService;
        this.incidentActionRecommendationService = incidentActionRecommendationService;
        this.observationSupport = observationSupport;
    }

    public IncidentResponse executeStandard(Incident incident, RoutingDecision decision) {
        List<String> runbookSnippets = loadKnowledge(incident);
        List<SimilarIncidentSummary> similarIncidents = loadHistory(incident);

        String escalationPolicy = resolvePolicy(incident);

        return analyzeAndBuildResponse(incident,
                                        decision,
                                        runbookSnippets,
                                        similarIncidents,
                                        escalationPolicy,
                                        null);
    }

    public IncidentResponse executeCritical(Incident incident, RoutingDecision decision) {
        // CRITICAL route는 AI 분석보다 먼저
        // 필수 escalation policy가 존재하는지 확인한다.
        //
        // 정책이 없으면 일반 분석으로 계속 진행하지 않는다.
        // 심각한 Incident를 정책 없이 AI 판단에 맡기지 않기 위한
        // 결정론적인 사전 통제 지점이다.
        String escalationPolicy = resolveRequiredCriticalPolicy(incident);

        observationSupport.mark(CRITICAL_ESCALATION_POLICY_RESOLVED, incident.incidentId().toString());

        List<String> runbookSnippets = loadKnowledge(incident);

        List<SimilarIncidentSummary> similarIncidents = loadHistory(incident);

        String criticalHandlingNote = """
                Critical escalation route selected.
                Review the escalation policy and treat the
                recommendations as guidance for the
                responsible incident responders.
                """;

        return analyzeAndBuildResponse(incident,
                                        decision,
                                        runbookSnippets,
                                        similarIncidents,
                                        escalationPolicy,
                                        criticalHandlingNote);
    }

    private List<String> loadKnowledge(Incident incident) {
        List<String> runbookSnippets =
                incidentKnowledgePort.findRelevantRunbookSnippets(
                                                                    incident.sourceSystem(),
                                                                    incident.summary(),
                                                                    incident.symptoms(),
                                                                    incident.severity());

        observationSupport.mark(KNOWLEDGE_LOADED, incident.incidentId().toString());

        return runbookSnippets;
    }

    private List<SimilarIncidentSummary> loadHistory(Incident incident) {
        List<SimilarIncidentSummary> similarIncidents =
                incidentHistoryPort.findSimilarIncidents(incident.sourceSystem(), incident.summary(), incident.severity());

        observationSupport.mark(HISTORY_LOADED, incident.incidentId().toString());

        return similarIncidents;
    }

    private String resolvePolicy(Incident incident) {
        String escalationPolicy = alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());

        observationSupport.mark(POLICY_RESOLVED, incident.incidentId().toString());

        return escalationPolicy;
    }

    private String resolveRequiredCriticalPolicy(Incident incident) {
        String escalationPolicy = alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());

        if (escalationPolicy == null || escalationPolicy.isBlank()) {

            throw new IllegalStateException("Critical incident requires an escalation policy.");
        }

        observationSupport.mark(IncidentObservationEvents.POLICY_RESOLVED, incident.incidentId().toString());

        return escalationPolicy;
    }

    private IncidentResponse analyzeAndBuildResponse(Incident incident,
                                                     RoutingDecision decision,
                                                     List<String> runbookSnippets,
                                                     List<SimilarIncidentSummary> similarIncidents,
                                                     String escalationPolicy,
                                                     String additionalResponseNote) {
        var analysisResult =
                incidentAnalysisService.analyze(incident, runbookSnippets, similarIncidents, escalationPolicy);

        observationSupport.mark(ANALYSIS_COMPLETED, incident.incidentId().toString());

        var actionBundle =
                incidentActionRecommendationService.recommend(incident, analysisResult, escalationPolicy);

        observationSupport.mark(ACTIONS_RECOMMENDED, incident.incidentId().toString());

        List<RecommendedActionItem> recommendedActions = actionBundle.actions();

        String responseNote = mergeResponseNote(actionBundle.responseNote(), additionalResponseNote);

        return new IncidentResponse(
                incident.incidentId().toString(),
                incident.severity().name(),
                decision.route().name(),
                decision.reason(),
                analysisResult,
                recommendedActions,
                similarIncidents,
                escalationPolicy,
                responseNote
        );
    }

    private String mergeResponseNote(String aiResponseNote, String additionalResponseNote) {
        if (additionalResponseNote == null
                || additionalResponseNote.isBlank()) {
            return aiResponseNote;
        }

        if (aiResponseNote == null
                || aiResponseNote.isBlank()) {
            return additionalResponseNote;
        }

        return aiResponseNote
                + System.lineSeparator()
                + additionalResponseNote;
    }
}
