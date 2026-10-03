package com.itvillage.incident.adapter.out.policy;

import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.dto.EscalationDecision;
import com.itvillage.incident.port.out.AlertPolicyPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalAlertPolicyAdapter implements AlertPolicyPort {

    private final AlertPolicyCatalog alertPolicyCatalog;

    public LocalAlertPolicyAdapter(AlertPolicyCatalog alertPolicyCatalog) {
        this.alertPolicyCatalog = alertPolicyCatalog;
    }

    @Override
    public EscalationDecision resolveEscalationPolicy(String sourceSystem, IncidentSeverity effectiveSeverity) {
        ServicePolicyDefinition servicePolicy = alertPolicyCatalog.findServicePolicy(sourceSystem);

        SeverityPolicyDefinition severityPolicy = alertPolicyCatalog.findSeverityPolicy(effectiveSeverity);

        String businessCriticality = servicePolicy == null ? "MEDIUM" : servicePolicy.businessCriticality();

        // 수업 13: 변경
        // 기존에는 구조화된 정책 값을 하나의 문자열로 평탄화해서 반환했다.
        // 이제는 Java 애플리케이션이 각 정책 값을 명시적으로 사용할 수 있도록 EscalationDecision으로 반환한다.
        return new EscalationDecision(
                sourceSystem,
                businessCriticality,
                effectiveSeverity,
                severityPolicy.acknowledgeWithinMinutes(),
                List.copyOf(severityPolicy.notifyChannels()),
                severityPolicy.requiresApprovalForRecoveryActions(),
                severityPolicy.responseGuidance());
    }

//    @Override
//    public String resolveEscalationPolicy(String sourceSystem, IncidentSeverity severity) {
//        ServicePolicyDefinition servicePolicy = alertPolicyCatalog.findServicePolicy(sourceSystem);
//
//        SeverityPolicyDefinition severityPolicy = alertPolicyCatalog.findSeverityPolicy(severity);
//
//        String businessCriticality = servicePolicy == null ? "MEDIUM" : servicePolicy.businessCriticality();
//
//        // RISK POINT:
//        // 정책 YAML에는 다음 데이터가 이미 존재한다.
//        //
//        // - payment-service의 businessCriticality: CRITICAL
//        // - payment-service의 minimumSeverity: HIGH
//        // - HIGH/CRITICAL severity의 requiresHumanApproval: true
//        // - severity별 notifyChannels
//        //
//        // 하지만 baseline은 요청으로 들어온 severity를 그대로 사용한다.
//        //
//        // 예:
//        // payment-service + MEDIUM 요청
//        //   → minimumSeverity가 HIGH여도 MEDIUM severity 정책을 그대로 선택한다.
//        //
//        // 또한 requiresHumanApproval과 notifyChannels는
//        // 사람이 읽는 정책 문자열에만 포함된다.
//        //
//        // 개선 방향:
//        // 정책 문자열 반환
//        //   → 구조화된 EscalationDecision 반환
//        //
//        // 호출자 또는 외부 시스템은 이 결정의 approvalRequired,
//        // notifyChannels, warRoomRequired를 사용해
//        // 승인·알림·후속 실행 절차를 처리한다.
//        return """
//                Service: %s
//                Business criticality: %s
//                Severity: %s
//                Acknowledge within: %d minutes
//                Notify channels: %s
//                Human approval required: %s
//                Instruction: %s
//                """.formatted(
//                sourceSystem,
//                businessCriticality,
//                severity,
//                severityPolicy.acknowledgeWithinMinutes(),
//                String.join(", ", severityPolicy.notifyChannels()),
//                severityPolicy.requiresApprovalForRecoveryActions(),
//                severityPolicy.responseGuidance()
//        );
//    }
}
