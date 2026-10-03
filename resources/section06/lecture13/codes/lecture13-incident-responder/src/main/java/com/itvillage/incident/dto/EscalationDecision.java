package com.itvillage.incident.dto;

import com.itvillage.incident.domain.IncidentSeverity;
import java.util.List;

// 수업 13: 추가
public record EscalationDecision(String sourceSystem,
                                 String businessCriticality,
                                 IncidentSeverity severity,
                                 int acknowledgeWithinMinutes,
                                 List<String> notifyChannels,
                                 boolean approvalRequired,
                                 String instruction) {
}
