package com.itvillage.incident.adapter.out.ai;

import java.util.List;

public record IncidentAiActionOutput(List<ActionItem> recommendedActions,
                                     String responseNote) {
    // QUALITY POINT:
    // 이 record는 JSON 매핑에 성공한 이후의 액션 목록 구조만 표현한다.
    // 목록 전체가 운영에 사용할 기본 계약을 만족하는지는 보장하지 않는다.
    //
    // 현재도 가능한 문제:
    // - recommendedActions가 null 또는 빈 리스트다.
    // - 액션 개수가 너무 적거나 너무 많다.
    // - 동일한 actionTitle이 여러 번 반복된다.
    // - priority가 중복되어 우선순위를 해석하기 어렵다.
    // - responseNote가 비어 있거나 무의미하다.
    //
    // 개선 방향 1:
    // 액션 목록 개수, 중복, responseNote 등
    // 구조와 기본 계약을 검증한다.
    //
    // 개선 방향 2:
    // 액션 간 우선순위가 적절한지,
    // rationale이 incident 근거와 일치하는지,
    // 추천이 충분히 구체적인지를 Evaluator-Optimizer로 평가·개선한다.
    public record ActionItem(
            int priority,
            String actionTitle,
            String rationale,
            String ownerSuggestion) {
        // QUALITY POINT:
        // ActionItem은 액션 한 건의 타입 구조만 표현한다.
        // 각 필드가 기본 제약을 만족하거나 의미적으로 적절한지는 보장하지 않는다.
        //
        // 현재도 가능한 문제:
        // - priority가 1~3 같은 허용 범위를 벗어난다.
        // - actionTitle, rationale, ownerSuggestion이 비어 있다.
        // - rationale이 너무 짧거나 incident 근거와 무관하다.
        // - actionTitle이 모호해서 실제 운영자가 무엇을 해야 하는지 알 수 없다.
        //
        // 개선 방향 1:
        // priority 범위, 필수 필드, 빈 값,
        // 기본 문자열 길이 같은 계약을 검증한다.
        //
        // 개선 방향 2:
        // rationale의 근거 일치성,
        // 액션의 구체성, 우선순위의 적절성을 평가·개선한다.
    }
}
