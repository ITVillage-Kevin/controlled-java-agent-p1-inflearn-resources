package com.itvillage.incident.adapter.out.ai;

import java.util.List;

public record IncidentAiAnalysisOutput(String situationSummary,
                                       List<String> likelyCauses,
                                       String impactAssessment,
                                       String confidenceNote) {
    // QUALITY POINT:
    // 이 record는 JSON이 정상적으로 매핑된 경우의 타입 구조만 표현한다.
    // 각 필드가 운영에 사용할 만큼 유효하고 신뢰할 수 있는지는 보장하지 않는다.
    //
    // 현재도 가능한 문제:
    // - situationSummary 또는 impactAssessment가 비어 있거나 무의미하다.
    // - likelyCauses가 비어 있거나, 중복되거나, 현재 incident와 무관하다.
    // - confidenceNote가 분석 한계를 설명하지 않고 근거 없이 과도한 확신을 표현한다.
    //
    // 개선 방향 1:
    // - 필수 필드
    // - 빈 값
    // - 목록 개수
    // 같은 기본 출력 계약을 검증한다.
    //
    // 개선 방향 2:
    // - incident, runbook, history 근거와의 일치성
    // - 원인 후보의 구체성
    // - confidence 표현의 적절성
    // 을 Evaluator-Optimizer로 평가·개선한다.
}
