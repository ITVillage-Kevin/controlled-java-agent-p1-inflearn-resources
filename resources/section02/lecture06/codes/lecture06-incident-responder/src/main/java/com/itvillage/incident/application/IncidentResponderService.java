package com.itvillage.incident.application;

import com.itvillage.common.exception.IncidentException;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentStatus;
import com.itvillage.incident.dto.IncidentRequest;
import com.itvillage.incident.dto.IncidentResponse;
import com.itvillage.incident.port.in.RespondToIncidentUseCase;
import com.itvillage.incident.port.out.AlertPolicyPort;
import com.itvillage.incident.port.out.IncidentHistoryPort;
import com.itvillage.incident.port.out.IncidentKnowledgePort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class IncidentResponderService implements RespondToIncidentUseCase {

    private final IncidentKnowledgePort knowledgePort;
    private final IncidentHistoryPort historyPort;
    private final AlertPolicyPort alertPolicyPort;
    private final IncidentAnalysisService analysisService;
    private final IncidentActionRecommendationService actionRecommendationService;

    public IncidentResponderService(
            IncidentKnowledgePort knowledgePort,
            IncidentHistoryPort historyPort,
            AlertPolicyPort alertPolicyPort,
            IncidentAnalysisService analysisService,
            IncidentActionRecommendationService actionRecommendationService
    ) {
        this.knowledgePort = knowledgePort;
        this.historyPort = historyPort;
        this.alertPolicyPort = alertPolicyPort;
        this.analysisService = analysisService;
        this.actionRecommendationService = actionRecommendationService;
    }

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

        var similarIncidents =
                historyPort.findSimilarIncidents(incident.sourceSystem(), incident.summary());

        var escalationPolicy =
                alertPolicyPort.resolveEscalationPolicy(incident.sourceSystem(), incident.severity());


        // FLOW POINT:
        // knowledge, history, policy, analysis, action recommendation 단계가
        // 실제 요청 파이프라인에서 어느 순서와 시간으로 실행됐는지
        // 관찰 가능한 span / event로 남기지 않는다.
        //
        // AiObservationSupport.mark(...) 메서드는 존재하지만,
        // 이 서비스와 하위 서비스 어디에서도 실제로 호출되지 않는다.
        //
        // 또한 TraceLoggingFilter의 traceId는 MDC 로그에만 남고,
        // 응답 헤더, 서비스 span, ChatClient, Langfuse trace까지
        // 표준 trace로 전파되지 않는다.
        //
        // 개선 방향:
        // Micrometer Tracing, OTEL, Spring AI Observation을 연결하고,
        // 각 단계에 observation event와 span을 추가한다.
        var analysis =
                analysisService.analyze(incident, runbookSnippets, similarIncidents, escalationPolicy);

        var actionBundle =
                actionRecommendationService.recommend(incident, analysis, escalationPolicy);

        return new IncidentResponse(
                incident.incidentId().toString(),
                incident.severity().name(),
                analysis,
                actionBundle.actions(),
                similarIncidents,
                escalationPolicy,
                actionBundle.responseNote()
        );
    }
}
