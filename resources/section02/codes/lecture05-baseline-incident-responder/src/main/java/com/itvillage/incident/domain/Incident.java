package com.itvillage.incident.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record Incident(UUID incidentId,
                       String sourceSystem,
                       String title,
                       String summary,
                       IncidentSeverity severity,
                       IncidentStatus status,
                       List<String> symptoms,
                       OffsetDateTime occurredAt) {
}
