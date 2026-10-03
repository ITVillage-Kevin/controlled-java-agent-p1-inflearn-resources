package com.itvillage.incident.application.routing;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentSeverity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Component
public class IncidentRouter {

    public RoutingDecision route(Incident incident) {
        // 가장 강한 조건부터 먼저 평가한다.
        //
        // 이번 수업에서는 별도의 priority 숫자를 만들지 않고
        // 코드 위에서 아래의 순서 자체를 Routing 우선순위로 사용한다.
        if (incident.severity() == IncidentSeverity.CRITICAL) {
            return new RoutingDecision(IncidentRoute.CRITICAL_ESCALATION, "severity-critical");
        }

        if (isPaymentFailure(incident)) {
            return new RoutingDecision(IncidentRoute.CRITICAL_ESCALATION, "payment-failure");
        }

        if (containsCustomerDataLossSignal(incident)) {
            return new RoutingDecision(IncidentRoute.CRITICAL_ESCALATION, "customer-data-loss");
        }

        if (isNotificationDelayOnly(incident)) {
            return new RoutingDecision(IncidentRoute.QUICK_TRIAGE, "notification-delay");
        }

        return new RoutingDecision(IncidentRoute.STANDARD_ANALYSIS, "default-standard");
    }

    private boolean isPaymentFailure(Incident incident) {
        if (!"payment-service".equalsIgnoreCase(incident.sourceSystem())) {
            return false;
        }

        return normalizedSymptoms(incident)
                .stream()
                .anyMatch(symptom ->
                        symptom.contains("payment authorization failure")
                                || symptom.contains("payment failure")
                                || symptom.contains("transaction failure"));
    }

    private boolean containsCustomerDataLossSignal(Incident incident) {
        return normalizedSymptoms(incident)
                .stream()
                .anyMatch(symptom ->
                        symptom.contains("customer data loss")
                                || symptom.contains("data corruption")
                                || symptom.contains("data breach"));
    }

    private boolean isNotificationDelayOnly(Incident incident) {
        if (incident.severity() != IncidentSeverity.LOW) {
            return false;
        }

        if (!"notification-service".equalsIgnoreCase(incident.sourceSystem())) {
            return false;
        }

        List<String> symptoms = normalizedSymptoms(incident);

        // 빈 Stream에서 allMatch(...)가 true가 되는 것을 막는다.
        //
        // symptoms가 없는데 QUICK_TRIAGE로 분류되는 상황을
        // 방지하기 위한 명시적인 방어 조건이다.
        if (symptoms.isEmpty()) {
            return false;
        }

        // "symptoms": [
        //    "notification delivery delay",
        //    "queue lag"
        // ]
        //
        // "symptoms": [
        //    "notification delivery delay",
        //    "message delivery failure"
        // ]
        return symptoms.stream()
                .allMatch(symptom ->
                                symptom.contains("delay")
                                || symptom.contains("latency")
                                || symptom.contains("queue lag"));
    }

    private List<String> normalizedSymptoms(Incident incident) {
        if (incident.symptoms() == null) {
            return List.of();
        }

        return incident.symptoms()
                .stream()
                .filter(Objects::nonNull)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .toList();
    }
}
