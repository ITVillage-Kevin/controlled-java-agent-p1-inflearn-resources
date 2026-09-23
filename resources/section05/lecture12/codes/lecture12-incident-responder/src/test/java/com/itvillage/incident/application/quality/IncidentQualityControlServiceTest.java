package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.domain.IncidentStatus;
import com.itvillage.observability.AiObservationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class IncidentQualityControlServiceTest {

    @Mock
    private IncidentQualityEvaluator evaluator;

    @Mock
    private IncidentQualityOptimizer optimizer;

    @Mock
    private IncidentAnalysisOutputValidator analysisValidator;

    @Mock
    private IncidentActionOutputValidator actionValidator;

    @Mock
    private AiObservationSupport observationSupport;

    private IncidentQualityControlService qualityControlService;

    @BeforeEach
    void setUp() {
        qualityControlService =
                new IncidentQualityControlService(
                        evaluator,
                        optimizer,
                        analysisValidator,
                        actionValidator,
                        observationSupport
                );
    }

    /*
     * 테스트 시나리오
     *
     * Initial Evaluator  → NEEDS_REVISION
     * Optimizer          → 수정 결과 생성
     * Basic Validator    → PASS
     * Re-Evaluator       → NEEDS_REVISION
     *
     * 기대 결과
     *
     * → NOT_RESOLVED
     * → initial Analysis 유지
     * → Optimizer는 정확히 1회만 호출
     * → 추가 품질 개선을 시도하지 않고 종료
     */
    @Test
    void shouldStopWithNotResolvedWhenReEvaluationStillNeedsRevision() {
        // given
        Incident incident =
                new Incident(
                        UUID.randomUUID(),
                        "order-service",
                        "Order processing latency increased",
                        "Order requests are taking longer than expected.",
                        IncidentSeverity.HIGH,
                        IncidentStatus.OPEN,
                        List.of(
                                "high latency",
                                "database connection wait"
                        ),
                        OffsetDateTime.now()
                );

        IncidentAiAnalysisOutput initialAnalysis =
                new IncidentAiAnalysisOutput(
                        "Order processing latency has increased.",
                        List.of(
                                "Database connection contention may be contributing."
                        ),
                        "Order processing may be delayed for users.",
                        "The available evidence is limited."
                );

        IncidentQualityEvaluation initialEvaluation =
                new IncidentQualityEvaluation(
                        IncidentQualityDecision.NEEDS_REVISION,
                        List.of(
                                "The likely cause is not sufficiently specific."
                        ),
                        List.of(
                                "Make the likely cause more specific "
                                        + "without claiming an unverified root cause."
                        )
                );

        IncidentAiAnalysisOutput optimizedAnalysis =
                new IncidentAiAnalysisOutput(
                        "Order processing latency has increased.",
                        List.of(
                                "Database connection contention may be contributing "
                                        + "to the increased latency."
                        ),
                        "Users may experience delayed order processing.",
                        "The evidence suggests a possible database connection issue, "
                                + "but the root cause is not confirmed."
                );

        IncidentQualityEvaluation reEvaluation =
                new IncidentQualityEvaluation(
                        IncidentQualityDecision.NEEDS_REVISION,
                        List.of(
                                "The analysis still does not provide "
                                        + "sufficient evidence alignment."
                        ),
                        List.of(
                                "Tie the likely cause more directly "
                                        + "to the available incident evidence."
                        )
                );

        // 첫 번째 평가는 품질 개선이 필요한 상태로 설정한다.
        given(evaluator.evaluateAnalysis(incident, initialAnalysis))
                .willReturn(initialEvaluation);

        // Optimizer는 수정된 Analysis를 반환한다.
        given(optimizer.optimizeAnalysis(incident, initialAnalysis, initialEvaluation))
                .willReturn(optimizedAnalysis);

        // 수정 결과는 수업 11의 기본 계약을 정상적으로 통과한다.
        given(analysisValidator.validate(optimizedAnalysis))
                .willReturn(ValidationResult.success());

        // 하지만 재평가에서도 의미 품질 문제가 해결되지 않은 것으로 설정한다.
        given(evaluator.evaluateAnalysis(incident, optimizedAnalysis))
                .willReturn(reEvaluation);

        // when
        QualityControlledResult<IncidentAiAnalysisOutput> result =
                                    qualityControlService.improveAnalysis(incident, initialAnalysis);

        // then

        // 품질 문제가 해결되지 않았으므로 NOT_RESOLVED로 종료한다.
        assertThat(result.status()).isEqualTo(QualityControlStatus.NOT_RESOLVED);

        // 검증되지 않은 Optimizer 결과를 채택하지 않고
        // 마지막으로 기본 조건이 보장된 initial Analysis를 유지한다.
        assertThat(result.output()).isEqualTo(initialAnalysis);

        // 수정 기회는 최대 1회만 허용했는지 확인한다.
        // Re-Evaluator가 다시 NEEDS_REVISION을 반환하더라도
        // Optimizer를 추가 호출하지 않는다.
        then(optimizer)
                .should(times(1))
                .optimizeAnalysis(incident, initialAnalysis, initialEvaluation);

        // Optimizer 결과는 실제로 한 번 재평가되었는지 확인한다.
        then(evaluator)
                .should(times(1))
                .evaluateAnalysis(incident, optimizedAnalysis);

        // 최종 품질 통제 상태가 Observation event로 기록되는지 확인한다.
        then(observationSupport)
                .should(times(1))
                .mark("analysis-quality-not-resolved", incident.incidentId().toString());
    }
}
