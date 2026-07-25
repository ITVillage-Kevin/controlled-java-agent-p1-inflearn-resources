package com.itvillage.incident.domain;

import java.time.OffsetDateTime;

public record IncidentTimelineEvent(OffsetDateTime occurredAt,
                                    String title,
                                    String description) {
}
