package com.itvillage.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiObservationSupport {

    private static final Logger log = LoggerFactory.getLogger(AiObservationSupport.class);

    public void mark(String stage, String incidentId) {
        // FLOW POINT:
        // 관찰을 위한 메서드 자체는 존재한다.
        //
        // 하지만 baseline에서는 IncidentResponderService,
        // IncidentAnalysisService,
        // IncidentActionRecommendationService 어느 곳에서도
        // 이 mark(...) 메서드를 실제로 호출하지 않는다.
        //
        // 따라서 코드상 "관찰 지점"은 있지만,
        // 실행 파이프라인에서는 죽어 있는 상태다.
        //
        // 개선 방향:
        // - @Observed
        // - custom span
        // - AiObservationSupport.mark(...)
        // 를 서비스 호출 파이프라인에 실제 연결한다.
        log.info("AI observation marker. stage={}, incidentId={}", stage, incidentId);
    }
}
