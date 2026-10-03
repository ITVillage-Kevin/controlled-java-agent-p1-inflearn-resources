package com.itvillage.incident.adapter.out.history;

import java.time.Instant;
import java.util.List;

public record HistoricalIncidentRecord(String incidentId,
                                       String sourceSystem,
                                       String title,
                                       List<String> symptoms,
                                       String resolutionSummary,
                                       Instant occurredAt) {
}
