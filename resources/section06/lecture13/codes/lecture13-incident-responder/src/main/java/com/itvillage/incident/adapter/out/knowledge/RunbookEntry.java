package com.itvillage.incident.adapter.out.knowledge;

import com.itvillage.incident.domain.IncidentSeverity;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public record RunbookEntry(String id,
                           String title,
                           int priority,
                           LocalDate updatedAt,
                           List<IncidentSeverity> severityHints,
                           List<String> keywords,
                           String deduplicationKey,
                           String content) {
    public boolean matches(String summary, List<String> symptoms) {
        Set<String> incidentTerms = Stream.concat(
                        Stream.of(summary == null ? "" : summary),
                        symptoms == null ? Stream.empty() : symptoms.stream()
                )
                .flatMap(text -> Stream.of(
                        text.toLowerCase(Locale.ROOT)
                                .split("[^a-z0-9.-]+")
                ))
                .filter(term -> !term.isBlank())
                .collect(java.util.stream.Collectors.toSet());

        return keywords.stream()
                .map(keyword -> keyword.toLowerCase(Locale.ROOT))
                .anyMatch(keyword ->
                        incidentTerms.contains(keyword)
                                || incidentTerms.stream()
                                .anyMatch(term -> keyword.contains(term))
                );
    }
}
