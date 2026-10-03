package com.itvillage.incident.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Incident(UUID incidentId,
                       String sourceSystem,
                       String title,
                       String summary,
                       IncidentSeverity requestedSeverity, // 수업 13: 추가 - 클라이언트가 최초 요청한 심각도
                       IncidentSeverity severity, // 수업 13: 변경 - 서비스별 최소 심각도 정책이 적용된 실제 처리 심각도
                       IncidentStatus status,
                       List<String> symptoms,
                       OffsetDateTime occurredAt) {
}
