package com.itvillage.incident.application.risk;

// 수업 13: 추가
public record ActionRiskAssessment(ActionRiskLevel riskLevel,
                                   boolean approvalRequired,
                                   String riskReason,
                                   String deduplicationKey,
                                   boolean allowed) {
}
