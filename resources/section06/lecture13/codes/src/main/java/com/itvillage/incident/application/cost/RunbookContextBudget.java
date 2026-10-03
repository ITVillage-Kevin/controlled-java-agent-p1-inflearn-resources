package com.itvillage.incident.application.cost;

import com.itvillage.incident.domain.IncidentSeverity;

// 수업 10: 추가
public record RunbookContextBudget(int maxEntries) {
    public static RunbookContextBudget from(IncidentSeverity severity) {
        return switch (severity) {
            case LOW -> new RunbookContextBudget(1);
            case MEDIUM -> new RunbookContextBudget(2);
            case HIGH -> new RunbookContextBudget(4);
            case CRITICAL -> new RunbookContextBudget(5);
        };
    }
}
