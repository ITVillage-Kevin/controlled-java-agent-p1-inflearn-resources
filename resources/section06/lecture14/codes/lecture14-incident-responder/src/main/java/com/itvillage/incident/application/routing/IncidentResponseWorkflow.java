package com.itvillage.incident.application.routing;

import com.itvillage.incident.application.IncidentActionRecommendationService;
import com.itvillage.incident.application.IncidentAnalysisService;
import com.itvillage.incident.application.risk.IncidentRiskPolicyService;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.EscalationDecision;
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

    // 수업 13: 추가
    // 품질 통제를 통과한 AI 추천에 위험 분류와 정책을 적용한다.
    private final IncidentRiskPolicyService incidentRiskPolicyService;
    private final IncidentKnowledgePort incidentKnowledgePort;
    private final IncidentHistoryPort incidentHistoryPort;
    private final AlertPolicyPort alertPolicyPort;
    private final IncidentAnalysisService incidentAnalysisService;
    private final IncidentActionRecommendationService incidentActionRecommendationService;
    private final AiObservationSupport observationSupport;

    public IncidentResponseWorkflow(IncidentRiskPolicyService incidentRiskPolicyService, // 수업 13: 추가
                                    IncidentKnowledgePort incidentKnowledgePort,
                                    IncidentHistoryPort incidentHistoryPort,
                                    AlertPolicyPort alertPolicyPort,
                                    IncidentAnalysisService incidentAnalysisService,
                                    IncidentActionRecommendationService incidentActionRecommendationService,
                                    AiObservationSupport observationSupport) {
        // 수업 13: 추가
        this.incidentRiskPolicyService = incidentRiskPolicyService;
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

        EscalationDecision escalationPolicy = resolvePolicy(incident);

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
        EscalationDecision escalationPolicy = resolveRequiredCriticalPolicy(incident);

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

    // 수업 13: 변경 - EscalationDecision로 타입 변경
    private EscalationDecision resolvePolicy(Incident incident) {
        // 수업 13: 변경 - EscalationDecision로 타입 변경
        EscalationDecision escalationPolicy = alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());

        observationSupport.mark(POLICY_RESOLVED, incident.incidentId().toString());

        return escalationPolicy;
    }

    // 수업 13: 변경 - EscalationDecision로 타입 변경
    private EscalationDecision resolveRequiredCriticalPolicy(Incident incident) {
        // 수업 13: 변경 - EscalationDecision로 타입 변경
        EscalationDecision escalationPolicy = alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());

        if (escalationPolicy == null) {

            throw new IllegalStateException("Critical incident requires an escalation policy.");
        }

        observationSupport.mark(IncidentObservationEvents.POLICY_RESOLVED, incident.incidentId().toString());

        return escalationPolicy;
    }

    private IncidentResponse analyzeAndBuildResponse(Incident incident,
                                                     RoutingDecision decision,
                                                     List<String> runbookSnippets,
                                                     List<SimilarIncidentSummary> similarIncidents,
                                                     EscalationDecision escalationPolicy,
                                                     String additionalResponseNote) {
        var analysisResult =
                incidentAnalysisService.analyze(incident, runbookSnippets, similarIncidents, escalationPolicy);

        observationSupport.mark(ANALYSIS_COMPLETED, incident.incidentId().toString());

        var actionBundle =
                incidentActionRecommendationService.recommend(incident, analysisResult, escalationPolicy);

        observationSupport.mark(ACTIONS_RECOMMENDED, incident.incidentId().toString());

        // 수업 13: 추가
        // 기본 계약 검증과 품질 통제를 통과한 AI 추천에 위험 분류 및 정책을 적용한다.
        var riskResult =
                incidentRiskPolicyService.apply(incident, actionBundle.actions(), actionBundle.responseNote());

        // 수업 13: 변경
        // 기존에는 AI 추천 목록을 그대로 최종 응답에 사용했다.
        // 이제는 위험 분류, 중복 제거 및 정책 적용을 거친 추천 목록을 최종 응답에 사용한다.
        List<RecommendedActionItem> recommendedActions = riskResult.actions();

//        List<RecommendedActionItem> recommendedActions = actionBundle.actions();

        // 수업 13: 변경 - 기존 AI 응답 설명 대신 위험 정책 적용 결과가 반영된 responseNote를 사용한다.
        String responseNote = mergeResponseNote(riskResult.responseNote(), additionalResponseNote);
//        String responseNote = mergeResponseNote(actionBundle.responseNote(), additionalResponseNote);

        // 수업 13: 변경 - 최종 응답에 정책 적용 후 추천 목록과 설명을 반영한다.
        return new IncidentResponse(
                        incident.incidentId().toString(),
                        incident.severity().name(),
                        incident.requestedSeverity().name(),
                        decision.route().name(),
                        decision.reason(),
                        analysisResult,
                        recommendedActions,
                        similarIncidents,
                        escalationPolicy,
                        responseNote);
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
