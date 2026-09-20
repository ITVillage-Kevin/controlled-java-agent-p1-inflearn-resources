package com.itvillage.incident.application.cost;

import com.itvillage.incident.domain.IncidentSeverity;

// 수업 10: 추가
public record HistoryContextBudget(int maxEntries) {
    public static HistoryContextBudget from(IncidentSeverity severity) {
        return switch (severity) {
            case LOW -> new HistoryContextBudget(0);
            case MEDIUM -> new HistoryContextBudget(1);
            case HIGH -> new HistoryContextBudget(3);
            case CRITICAL -> new HistoryContextBudget(4);
        };
    }
}
