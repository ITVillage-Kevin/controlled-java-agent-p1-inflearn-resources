package com.itvillage.incident.adapter.out.knowledge;

import com.itvillage.incident.port.out.IncidentKnowledgePort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocalRunbookKnowledgeAdapter implements IncidentKnowledgePort {

    private final RunbookCatalog runbookCatalog;

    public LocalRunbookKnowledgeAdapter(
            RunbookCatalog runbookCatalog
    ) {
        this.runbookCatalog = runbookCatalog;
    }

    @Override
    public List<String> findRelevantRunbookSnippets(
            String sourceSystem,
            String summary,
            List<String> symptoms
    ) {
        List<RunbookEntry> matchedEntries = runbookCatalog
                .findByService(sourceSystem)
                .stream()
                .filter(entry -> entry.matches(summary, symptoms))
                .toList();

        List<RunbookEntry> entriesToReturn = matchedEntries.isEmpty()
                ? runbookCatalog.findDefaultEntries()
                : matchedEntries;

        // COST POINT:
        // baseline은 관련 런북을 priority와 최신성 기준으로 정렬하지만,
        // 결과 개수, 전체 문자 수, 중복 여부, Prompt 예산을 제한하지 않는다.
        return runbookCatalog.sortByPriorityAndRecency(entriesToReturn)
                .stream()
                .map(RunbookEntry::content)
                .toList();
    }
}
