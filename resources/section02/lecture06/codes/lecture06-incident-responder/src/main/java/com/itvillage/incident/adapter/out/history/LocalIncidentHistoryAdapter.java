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
        // 관련 이력이 여러 건 검색되면 현재는 그 결과를 모두 반환한다.
        //
        // 즉 아직 다음과 같은 전달량 통제가 없다.
        //
        // - 유사 Incident의 최대 반환 개수
        // - Incident severity에 따른 서로 다른 Context 허용량
        //
        // 따라서 운영이 계속되어 과거 Incident 이력이 쌓일수록,
        // LLM Prompt에 전달되는 History Context도 함께 증가할 수 있다.
        //
        // 개선 방향:
        // - 기존 유사도 순위는 유지하면서
        // - Incident 특성에 따라 상위 이력만 선택하고,
        // - LLM에 전달할 개수에 명시적인 상한을 둔다.
        //
        // 구체적인 상한 값은
        // 실제 데이터와 token 사용량을 관찰한 뒤 결정한다.
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
