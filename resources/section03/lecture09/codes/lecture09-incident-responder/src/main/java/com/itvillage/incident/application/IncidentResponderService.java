package com.itvillage.incident.application;

import com.itvillage.incident.application.routing.IncidentRouteHandler;
import com.itvillage.incident.application.routing.IncidentRouteHandlerRegistry;
import com.itvillage.incident.application.routing.IncidentRouter;
import com.itvillage.incident.application.routing.RoutingDecision;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentStatus;
import com.itvillage.incident.dto.IncidentRequest;
import com.itvillage.incident.dto.IncidentResponse;
import com.itvillage.incident.port.in.RespondToIncidentUseCase;
import com.itvillage.observability.AiObservationSupport;
import com.itvillage.observability.IncidentObservationEvents;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.UUID;

import static com.itvillage.observability.IncidentObservationEvents.INCIDENT_CREATED;

@Service
public class IncidentResponderService implements RespondToIncidentUseCase {

    private final IncidentRouter incidentRouter;

    private final IncidentRouteHandlerRegistry handlerRegistry;

    private final AiObservationSupport observationSupport;

    public IncidentResponderService(IncidentRouter incidentRouter,
                                    IncidentRouteHandlerRegistry handlerRegistry,
                                    AiObservationSupport observationSupport) {
        this.incidentRouter = incidentRouter;

        this.handlerRegistry = handlerRegistry;

        this.observationSupport = observationSupport;
    }

    @Override
    @Observed(name = "incident.responder.respond", contextualName = "respond-incident")
    public IncidentResponse respond(IncidentRequest request) {
        Incident incident =
                new Incident(
                        UUID.randomUUID(),
                        request.sourceSystem(),
                        request.title(),
                        request.summary(),
                        request.severity(),
                        IncidentStatus.OPEN,
                        request.symptoms(),
                        OffsetDateTime.now()
                );

        try {
            observationSupport.mark(INCIDENT_CREATED, incident.incidentId().toString());

            // FLOW POINT
            //
            // baseline:
            // 모든 Incident가 동일한 선형 workflow를 실행했다.
            //
            // 수업 9:
            // Incident 특성으로 먼저 route를 선택한다.
            RoutingDecision decision = incidentRouter.route(incident);

            observationSupport.markRouteResolved(incident.incidentId().toString(),
                                                    decision.route().name(),
                                                    decision.reason());

            // Router가 "어디로 갈지" 결정했다면
            // Registry는 그 route를 담당하는 Handler를 찾는다.
            IncidentRouteHandler handler = handlerRegistry.getRequiredHandler(decision.route());

            // 선택된 Handler가 route별 실제 흐름을 실행한다.
            return handler.handle(incident, decision);

        } catch (RuntimeException exception) {
            observationSupport.recordError(exception);
            throw exception;
        }
    }
}

/*@Service
public class IncidentResponderService implements RespondToIncidentUseCase {

    private final IncidentKnowledgePort knowledgePort;
    private final IncidentHistoryPort historyPort;
    private final AlertPolicyPort alertPolicyPort;
    private final IncidentAnalysisService analysisService;
    private final IncidentActionRecommendationService actionRecommendationService;

    // 수업 7: 추가
    private final AiObservationSupport observationSupport;

    public IncidentResponderService(IncidentKnowledgePort knowledgePort,
                                    IncidentHistoryPort historyPort,
                                    AlertPolicyPort alertPolicyPort,
                                    IncidentAnalysisService analysisService,
                                    IncidentActionRecommendationService actionRecommendationService,
                                    AiObservationSupport observationSupport) {
        this.knowledgePort = knowledgePort;
        this.historyPort = historyPort;
        this.alertPolicyPort = alertPolicyPort;
        this.analysisService = analysisService;
        this.actionRecommendationService = actionRecommendationService;
        this.observationSupport = observationSupport;
    }

    @Observed(name = "incident.responder.respond", contextualName = "respond-incident")
    @Override
    public IncidentResponse respond(IncidentRequest request) {
        if (request.symptoms() == null || request.symptoms().isEmpty()) {
            throw new IncidentException("symptoms must not be empty");
        }

        Incident incident = new Incident(
                UUID.randomUUID(),
                request.sourceSystem(),
                request.title(),
                request.summary(),
                request.severity(),
                IncidentStatus.OPEN,
                request.symptoms(),
                OffsetDateTime.now()
        );

        try {
            observationSupport.mark(INCIDENT_CREATED, incident.incidentId().toString());

            // FLOW POINT:
            // 이 요청은 sourceSystem, severity, symptoms가 달라도
            // 모두 동일한 선형 처리 경로를 따른다.
            //
            // 현재 흐름:
            // knowledge 조회 → history 조회 → policy 조회
            // → analysis LLM 호출 → action recommendation LLM 호출 → 응답 반환
            //
            // LOW notification-service 지연과 CRITICAL payment-service 장애도
            // 동일한 흐름을 타며, 요청 특성에 따라 handler를 선택하는
            // routing / branching 구조가 없다.
            //
            // 개선 방향:
            // severity, sourceSystem, symptom을 기준으로
            // QUICK_TRIAGE(빠른 상태 확인: 낮은 우선순위),
            // STANDARD_ANALYSIS(표준 분석: 일반적인 장애),
            // CRITICAL_ESCALATION(긴급 에스컬레이션 대응: 높은 우선순위)  경로로 분기한다.
            var runbookSnippets =
                    knowledgePort.findRelevantRunbookSnippets(incident.sourceSystem(), incident.summary(), incident.symptoms());

            observationSupport.mark(KNOWLEDGE_LOADED, incident.incidentId().toString());

            var similarIncidents =
                    historyPort.findSimilarIncidents(incident.sourceSystem(), incident.summary());

            observationSupport.mark(HISTORY_LOADED, incident.incidentId().toString());

            var escalationPolicy =
                    alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());

            observationSupport.mark(POLICY_RESOLVED, incident.incidentId().toString());

            var analysis =
                    analysisService.analyze(incident, runbookSnippets, similarIncidents, escalationPolicy);

            observationSupport.mark(ANALYSIS_COMPLETED, incident.incidentId().toString());

            var actionBundle =
                    actionRecommendationService.recommend(incident, analysis, escalationPolicy);

            observationSupport.mark(ACTIONS_RECOMMENDED, incident.incidentId().toString());

            return new IncidentResponse(
                    incident.incidentId().toString(),
                    incident.severity().name(),
                    analysis,
                    actionBundle.actions(),
                    similarIncidents,
                    escalationPolicy,
                    actionBundle.responseNote()
            );
        } catch (RuntimeException exception) {
            // 현재 respond span에 error를 기록한다.
            observationSupport.recordError(exception);
            throw exception;
        }
    }
}*/
