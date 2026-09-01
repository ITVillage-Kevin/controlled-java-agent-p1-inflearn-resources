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
    public List<String> findRelevantRunbookSnippets(String sourceSystem, String summary, List<String> symptoms) {
        List<RunbookEntry> matchedEntries = runbookCatalog
                .findByService(sourceSystem)
                .stream()
                .filter(entry -> entry.matches(summary, symptoms))
                .toList();

        List<RunbookEntry> entriesToReturn = matchedEntries.isEmpty() ? runbookCatalog.findDefaultEntries() : matchedEntries;

        // COST POINT:
        // 이 adapter는 서비스별 런북을 읽고,
        // summary·symptoms로 관련 런북을 찾고,
        // priority·updatedAt 기준으로 정렬한다.
        //
        // 그러나 관련 런북이 여러 개면 모두 반환한다.
        //
        // 아직 없는 것:
        // - 반환 항목 상한
        // - 총 문자 수 상한
        // - 유사 런북 중복 제거
        // - severity별 서로 다른 context budget
        // - Prompt 전체 토큰 예산 계산
        //
        // 따라서 여러 키워드가 동시에 매칭되면,
        // 다수의 런북 본문이 IncidentPromptFactory로 전달된다.
        //
        // 개선 방향:
        // 관련 런북 상위 N개만 선택
        // 런북 본문 최대 길이 제한
        // 같은 문제군의 유사 런북 중복 제거
        return runbookCatalog.sortByPriorityAndRecency(entriesToReturn)
                .stream()
                .map(RunbookEntry::content)
                .toList();
    }
}
