package com.itvillage.observability;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AiObservationSupport {
    private static final Logger log = LoggerFactory.getLogger(AiObservationSupport.class);

    //-- 수업 7: 추가 시작
    private final ObservationRegistry observationRegistry;

    //-- 수업 7: 추가 끝.
    public AiObservationSupport(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    // 수업 7: 수정
    public void mark(String stage, String incidentId) {
        // 현재 실행 중인 Observation을 가져온다.
        // @Observed가 생성한 현재 service span에 해당한다.
        Observation currentObservation = observationRegistry.getCurrentObservation();

        if (currentObservation == null) {
            log.warn("incident-observation-event-skipped. stage={}, incidentId={}", stage, incidentId);
            return;
        }

        // incidentId는 요청마다 달라지는 high-cardinality 값이다.
        //
        // 이 값은 metric tag로 사용하지 않고,
        // 개별 요청 trace를 찾기 위한 high-cardinality span attribute로만 기록한다.
        currentObservation.highCardinalityKeyValue("incident.id", incidentId);

        // 현재 service span 안에 업무 이정표 event를 추가한다.
        //
        // 예:
        // - knowledge-loaded
        // - analysis-prompt-built
        // - actions-recommended
        //
        // 이 event는 단순 로그 문구가 아니라
        // span의 시간 흐름 안에 남는 구조화된 이정표다.
        currentObservation.event(Observation.Event.of(stage));


        // 수업 7에서는 외부 trace backend를 아직 연결하지 않으므로,
        // 로컬 콘솔에서 event 호출 순서를 확인할 수 있도록 짧은 구조화 로그를 남긴다.
        //
        // logging pattern이 MDC traceId를 출력하도록 구성되어 있으면,
        // 이 로그에도 현재 요청의 traceId가 자동으로 함께 출력된다.
        log.info("incident-observation-event. stage={}, incidentId={}", stage, incidentId);
    }

    public void recordError(RuntimeException exception) {
        Observation currentObservation =
                observationRegistry.getCurrentObservation();

        // 현재 Observation이 존재하면 오류를 기록한다.
        //
        // 이 메서드는 예외를 숨기거나 fallback으로 바꾸지 않는다.
        // 호출한 서비스는 오류 기록 후 원래 예외를 다시 던진다.
        if (currentObservation != null) {
            currentObservation.error(exception);
        }
    }

    // 수업 6에서 사용한 mark()
//    public void mark(String stage, String incidentId) {
//        // FLOW POINT: 개선 완료
//        // 관찰을 위한 메서드 자체는 존재한다.
//        //
//        // 하지만 baseline에서는 IncidentResponderService,
//        // IncidentAnalysisService,
//        // IncidentActionRecommendationService 어느 곳에서도
//        // 이 mark(...) 메서드를 실제로 호출하지 않는다.
//        //
//        // 따라서 코드상 "관찰 지점"은 있지만,
//        // 실행 파이프라인에서는 죽어 있는 상태다.
//        //
//        // 개선 방향:
//        // - @Observed
//        // - custom span
//        // - AiObservationSupport.mark(...)
//        // 를 서비스 호출 파이프라인에 실제 연결한다.
//        log.info("AI observation marker. stage={}, incidentId={}", stage, incidentId);
//    }
}
