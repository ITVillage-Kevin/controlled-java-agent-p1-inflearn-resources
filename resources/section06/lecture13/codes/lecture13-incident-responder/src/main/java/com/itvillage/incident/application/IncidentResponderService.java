package com.itvillage.incident.application;

import com.itvillage.incident.application.risk.IncidentSeverityPolicy;
import com.itvillage.incident.application.routing.IncidentRouteHandler;
import com.itvillage.incident.application.routing.IncidentRouteHandlerRegistry;
import com.itvillage.incident.application.routing.IncidentRouter;
import com.itvillage.incident.application.routing.RoutingDecision;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentSeverity;
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

    // 수업 13: 추가 - 서비스별 최소 심각도 정책을 적용하기 위한 의존성
    private final IncidentSeverityPolicy severityPolicy;

    private final IncidentRouteHandlerRegistry handlerRegistry;

    private final AiObservationSupport observationSupport;

    public IncidentResponderService(IncidentSeverityPolicy severityPolicy, // 수업 13: 추가
                                    IncidentRouter incidentRouter,
                                    IncidentRouteHandlerRegistry handlerRegistry,
                                    AiObservationSupport observationSupport) {
        // 수업 13: 추가
        this.severityPolicy = severityPolicy;
        this.incidentRouter = incidentRouter;
        this.handlerRegistry = handlerRegistry;
        this.observationSupport = observationSupport;
    }

    @Override
    @Observed(name = "incident.responder.respond", contextualName = "respond-incident")
    public IncidentResponse respond(IncidentRequest request) {
        // 수업 13: 추가 - 클라이언트가 최초 요청한 심각도
        IncidentSeverity requestedSeverity = request.severity();

        // 수업 13: 추가 - 서비스별 최소 심각도 정책을 적용한 실제 처리 심각도
        IncidentSeverity effectiveSeverity =
                severityPolicy.effectiveSeverity(request.sourceSystem(), request.severity());

        Incident incident =
                new Incident(
                        UUID.randomUUID(),
                        request.sourceSystem(),
                        request.title(),
                        request.summary(),
                        requestedSeverity, // 수업 13: 변경
                        effectiveSeverity, // 수업 13: 변경
                        IncidentStatus.OPEN,
                        request.symptoms(),
                        OffsetDateTime.now()
                );

        try {
            observationSupport.mark(INCIDENT_CREATED, incident.incidentId().toString());

            // Incident 특성으로 먼저 route를 선택한다.
            RoutingDecision decision = incidentRouter.route(incident);

            observationSupport.markRouteResolved(incident.incidentId().toString(),
                                                    decision.route().name(),
                                                    decision.reason());

            // Router가 "어디로 갈지" 결정했다면
            // Registry는 그 route를 담당하는 Handler를 찾는다.
            IncidentRouteHandler handler = handlerRegistry.getRequiredHandler(decision.route());

            // 선택된 Handler가 route별 실제 흐름을 실행한다.
            IncidentResponse response = handler.handle(incident, decision);

            // 수업 13: 변경
            // Handler가 반환한 응답에 요청 당시의 원본 심각도를 추가한다.
            // response.severity(): 서비스별 최소 심각도 정책이 적용된 실제 처리 심각도
            // request.severity().name(): 클라이언트가 처음 요청한 원본 심각도
            // 기존 분석, 추천, 라우팅 정보는 그대로 유지한다.
            return new IncidentResponse(response.incidentId(),
                                        response.severity(),
                                        request.severity().name(),
                                        response.route(),
                                        response.routeReason(),
                                        response.analysis(),
                                        response.recommendedActions(),
                                        response.similarIncidents(),
                                        response.escalationPolicy(),
                                        response.responseNote());

        } catch (RuntimeException exception) {
            observationSupport.recordError(exception);
            throw exception;
        }
    }
}
