package com.itvillage.incident.adapter.out.history;

import com.itvillage.incident.dto.SimilarIncidentSummary;
import com.itvillage.incident.port.out.IncidentHistoryPort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

@Component
public class LocalIncidentHistoryAdapter implements IncidentHistoryPort {

    private final IncidentHistoryCatalog incidentHistoryCatalog;

    public LocalIncidentHistoryAdapter(
            IncidentHistoryCatalog incidentHistoryCatalog
    ) {
        this.incidentHistoryCatalog = incidentHistoryCatalog;
    }

    @Override
    public List<SimilarIncidentSummary> findSimilarIncidents(String sourceSystem, String summary) {
        Set<String> queryTerms = extractTerms(summary);

        // COST POINT:
        // baseline은 유사도가 있는 과거 장애를 모두 반환한다.
        // 반환 개수와 resolutionSummary 길이를 제한하지 않는다.
        return incidentHistoryCatalog.findBySourceSystem(sourceSystem)
                .stream()
                .map(incident -> new ScoredIncident(
                        incident,
                        similarityScore(incident, queryTerms)
                ))
                .filter(scoredIncident -> scoredIncident.score() > 0)
                .sorted(
                        Comparator.comparingInt(ScoredIncident::score)
                                .reversed()
                                .thenComparing(
                                        scoredIncident -> scoredIncident.incident().occurredAt(),
                                        Comparator.reverseOrder()
                                )
                )
                .map(scoredIncident -> new SimilarIncidentSummary(
                        scoredIncident.incident().incidentId(),
                        scoredIncident.incident().title(),
                        scoredIncident.incident().resolutionSummary()
                ))
                .toList();
    }

    private int similarityScore(
            HistoricalIncidentRecord incident,
            Set<String> queryTerms
    ) {
        return Stream.concat(
                        Stream.of(incident.title()),
                        incident.symptoms().stream()
                )
                .flatMap(text -> Stream.of(
                        text.toLowerCase(Locale.ROOT)
                                .split("[^a-z0-9.-]+")
                ))
                .filter(queryTerms::contains)
                .distinct()
                .mapToInt(ignored -> 1)
                .sum();
    }

    private Set<String> extractTerms(String summary) {
        return Stream.of(summary == null ? "" : summary)
                .flatMap(text -> Stream.of(
                        text.toLowerCase(Locale.ROOT)
                                .split("[^a-z0-9.-]+")
                ))
                .filter(term -> !term.isBlank())
                .collect(java.util.stream.Collectors.toSet());
    }

    private record ScoredIncident(
            HistoricalIncidentRecord incident,
            int score
    ) {
    }
}
