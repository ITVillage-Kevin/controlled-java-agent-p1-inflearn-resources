package com.itvillage.incident.application.risk;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.domain.IncidentStatus;
import com.itvillage.incident.dto.RecommendedActionItem;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentRiskPolicyServiceTest {

    private final IncidentRiskPolicyService service = new IncidentRiskPolicyService(new ActionRiskClassifier());

    // 서로 다른 restart 표현이 같은 `service-restart` 키로 분류되어 하나만 남는지,
    // 남은 restart 추천에 `HIGH / approvalRequired=true`가 추가되는지 확인한다.
    @Test
    void restart_synonyms_are_deduplicated_and_risk_metadata_is_added() {
        Incident incident = incident();

        List<RecommendedActionItem> actions = List.of(
                new RecommendedActionItem(
                        1,
                        "Restart payment-service pods",
                        "Restart the affected pods to recover service health.",
                        "On-call engineer"
                ),
                new RecommendedActionItem(
                        2,
                        "Restart the payment-service instances",
                        "Restart the affected instances after reviewing telemetry.",
                        "On-call engineer"
                ),
                new RecommendedActionItem(
                        3,
                        "Check database connection pool metrics",
                        "Inspect current connection pool saturation and wait time.",
                        "Database engineer"
                )
        );

        RiskControlledActions result =
                service.apply(incident, actions, "Original note.");

        assertThat(result.actions()).hasSize(2);
        assertThat(result.excludedCount()).isEqualTo(1);

        assertThat(result.actions().getFirst().priority()).isEqualTo(1);
        assertThat(result.actions().getFirst().riskLevel()).isEqualTo("HIGH");
        assertThat(result.actions().getFirst().approvalRequired()).isTrue();

        assertThat(result.actions().get(1).priority()).isEqualTo(2);
        assertThat(result.actions().get(1).riskLevel()).isEqualTo("LOW");
        assertThat(result.actions().get(1).approvalRequired()).isFalse();
    }

    // 기본 계약상 3개 추천이 존재하더라도 세 추천이 모두 파괴적 작업으로 분류되면 최종 응답 후보가 0개가 될 수 있고,
    // 수동 검토 안내가 추가되는지 확인한다.
    @Test
    void all_destructive_actions_are_excluded_and_manual_review_is_requested() {
        Incident incident = incident();

        List<RecommendedActionItem> actions = List.of(
                new RecommendedActionItem(
                        1,
                        "Delete database records",
                        "Delete database records related to the incident.",
                        "Database engineer"
                ),
                new RecommendedActionItem(
                        2,
                        "Drop table containing corrupted data",
                        "Drop table containing corrupted data before recovery.",
                        "Database engineer"
                ),
                new RecommendedActionItem(
                        3,
                        "Disable audit logging",
                        "Disable audit logging during recovery.",
                        "Platform engineer"
                )
        );

        RiskControlledActions result = service.apply(incident, actions, "Original note.");

        assertThat(result.actions()).isEmpty();
        assertThat(result.excludedCount()).isEqualTo(3);
        assertThat(result.responseNote()).contains("Review the incident manually");
    }

    private Incident incident() {
        return new Incident(
                UUID.randomUUID(),
                "payment-service",
                "Payment incident",
                "Payment processing is degraded.",
                IncidentSeverity.MEDIUM,
                IncidentSeverity.HIGH,
                IncidentStatus.OPEN,
                List.of("payment processing latency increased"),
                OffsetDateTime.now()
        );
    }
}
