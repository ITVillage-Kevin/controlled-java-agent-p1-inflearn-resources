package com.itvillage.incident.application.routing;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentResponse;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QuickTriageIncidentRouteHandler implements IncidentRouteHandler {

    @Override
    public IncidentRoute route() {
        return IncidentRoute.QUICK_TRIAGE;
    }

    @Override
    @Observed(name = "incident.routing.quick-triage", contextualName = "route-quick-triage")
    public IncidentResponse handle(Incident incident, RoutingDecision decision) {
        // QUICK route에서는 AI를 호출하지 않는다.
        //
        // 이것은 "LOW는 항상 중요하지 않다"는 의미가 아니다.
        // IncidentRouter의 제한적인 조건을 모두 통과한
        // notification delay-only 요청에만 적용한다.
        return new IncidentResponse(incident.incidentId().toString(),
                                        incident.severity().name(),
                                        decision.route().name(),
                                        decision.reason(),
                                        null,
                                        List.of(),
                                        List.of(),
                                        null,
                                        """
                                        Low-impact notification delay detected.
                                        Monitor queue lag and delivery latency.
                                        Escalate if impact or severity increases.
                                        """
        );
    }
}