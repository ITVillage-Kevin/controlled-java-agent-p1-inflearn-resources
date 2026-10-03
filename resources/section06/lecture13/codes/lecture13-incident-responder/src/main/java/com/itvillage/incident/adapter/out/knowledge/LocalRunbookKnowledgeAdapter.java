package com.itvillage.incident.adapter.out.knowledge;

import com.itvillage.incident.application.cost.RunbookContextBudget;
import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.port.out.IncidentKnowledgePort;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class LocalRunbookKnowledgeAdapter implements IncidentKnowledgePort {

    private final RunbookCatalog runbookCatalog;

    public LocalRunbookKnowledgeAdapter(RunbookCatalog runbookCatalog) {
        this.runbookCatalog = runbookCatalog;
    }

    @Override
    public List<String> findRelevantRunbookSnippets(String sourceSystem,
                                                    String summary,
                                                    List<String> symptoms,
                                                    IncidentSeverity severity) {

        // 수업 10: 추가
        RunbookContextBudget budget = RunbookContextBudget.from(severity);

        List<RunbookEntry> matchedEntries = runbookCatalog
                .findByService(sourceSystem)
                .stream()
                .filter(entry -> entry.matches(summary, symptoms))
                .toList();

        List<RunbookEntry> entriesToReturn = matchedEntries.isEmpty() ? runbookCatalog.findDefaultEntries() : matchedEntries;

        // COST POINT:
        // Runbook이 여러 개 검색되면 현재는 그 결과를 모두 반환한다.
        //
        // 즉 아직 다음과 같은 전달량 통제가 없다.
        //
        // - 관련 Runbook의 최대 반환 개수
        // - 같은 문제군에 속하는 유사 Runbook의 중복 제거
        // - Incident severity에 따른 서로 다른 Context 허용량
        //
        // 따라서 운영이 계속되어 Runbook이 늘어나거나,
        // 하나의 Incident에 여러 Runbook이 동시에 매칭되면
        // LLM Prompt로 전달되는 Context도 함께 증가할 수 있다.
        //
        // 개선 방향:
        // - 검색된 Runbook의 우선순위와 관련성을 유지하면서 같은 문제군의 중복을 줄이고,
        // - Incident 특성에 따라 LLM에 전달할 개수에 명시적인 상한을 둔다.
        //
        // 구체적인 상한 값과 선택 정책은
        // 실제 데이터 구조와 token 사용량을 관찰한 뒤 결정한다.
//        return runbookCatalog.sortByPriorityAndRecency(entriesToReturn)
//                .stream()
//                .map(RunbookEntry::content)
//                .toList();
        return runbookCatalog
                .sortByPriorityAndRecency(entriesToReturn)
                .stream()

                // 수업 10: 추가
                // 1. priority / updatedAt 정렬 결과를 유지하면서 같은 문제군의 중복 Runbook을 제거한다.
                .collect(
                Collectors.toMap(
                        this::deduplicationKey,
                        Function.identity(),  // database-connectivity -> RunbookEntry
                        (existing, incoming) ->
                                existing, LinkedHashMap::new))
                .values()
                .stream()

                // 수업 10: 추가
                // 2. severity 정책이 허용한 수까지만 LLM Context로 전달한다.
                .limit(budget.maxEntries())

                // 수업 10: 추가
                // 3. 현재 sample data의 entry 자체가 이미 작은 Context 단위이므로 본문을 runtime에서 임의 truncate하지 않는다.
                .map(RunbookEntry::content)
                .filter(content -> content != null && !content.isBlank())
                .toList();
    }

    // 수업 10: 추가
    private String deduplicationKey(RunbookEntry entry) {
        if (entry.deduplicationKey() == null || entry.deduplicationKey().isBlank()) {
            // deduplicationKey가 없는 데이터는 서로 다른 Runbook으로 취급한다.
            return "id:" + entry.id();
        }

        return entry
                .deduplicationKey()
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}
