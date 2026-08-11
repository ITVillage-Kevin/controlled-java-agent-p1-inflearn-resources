package com.itvillage.incident.adapter.out.policy;

import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.port.out.AlertPolicyPort;
import org.springframework.stereotype.Component;

@Component
public class LocalAlertPolicyAdapter implements AlertPolicyPort {

    private final AlertPolicyCatalog alertPolicyCatalog;

    public LocalAlertPolicyAdapter(AlertPolicyCatalog alertPolicyCatalog) {
        this.alertPolicyCatalog = alertPolicyCatalog;
    }

    @Override
    public String resolveEscalationPolicy(String sourceSystem, IncidentSeverity severity) {
        ServicePolicyDefinition servicePolicy =
                alertPolicyCatalog.findServicePolicy(sourceSystem);

        SeverityPolicyDefinition severityPolicy =
                alertPolicyCatalog.findSeverityPolicy(severity);

        String businessCriticality = servicePolicy == null
                ? "MEDIUM"
                : servicePolicy.businessCriticality();

        // RISK POINT:
        // YAML에는 서비스 중요도와 minimumSeverity가 정의되어 있다.
        // 하지만 baseline은 요청으로 들어온 severity를 그대로 사용한다.
        // sourceSystem별 severity 승격, 승인 강제, 실제 알림 호출은 아직 연결하지 않는다.
        return """
                Service: %s
                Business criticality: %s
                Severity: %s
                Acknowledge within: %d minutes
                Notify channels: %s
                Human approval required: %s
                Instruction: %s
                """.formatted(
                sourceSystem,
                businessCriticality,
                severity,
                severityPolicy.acknowledgeWithinMinutes(),
                String.join(", ", severityPolicy.notifyChannels()),
                severityPolicy.requiresApprovalForRecoveryActions(),
                severityPolicy.responseGuidance()
        );
    }
}
