package com.itvillage.incident.domain;

import java.time.LocalDate;
import java.util.List;

public record RunbookEntry(String id,
                           String title,
                           int priority,
                           LocalDate updatedAt,
                           List<IncidentSeverity> severityHints,
                           List<String> keywords,
                           String content) {
}
