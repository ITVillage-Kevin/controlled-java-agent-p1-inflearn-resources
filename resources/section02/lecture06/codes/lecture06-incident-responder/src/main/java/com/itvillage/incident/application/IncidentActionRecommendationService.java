package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.RecommendedActionItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentActionRecommendationService {

    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentActionRecommendationService(IncidentPromptFactory promptFactory,
                                               IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public ActionBundle recommend(Incident incident,
                                  IncidentAnalysisResult analysisResult,
                                  String escalationPolicy) {
        var analysisOutput = new IncidentAiAnalysisOutput(
                analysisResult.situationSummary(),
                analysisResult.likelyCauses(),
                analysisResult.impactAssessment(),
                analysisResult.confidenceNote()
        );

        String prompt = promptFactory.buildActionPrompt(incident, analysisOutput, escalationPolicy);
        IncidentAiActionOutput output = chatGateway.generateActions(prompt);

        // QUALITY POINT:
        // JSON 매핑에 성공했더라도 recommendedActions의 개수,
        // priority 범위와 중복, actionTitle / rationale / ownerSuggestion의 빈 값,
        // responseNote의 존재 여부를 검증하지 않는다.
        //
        // 또한 ChatClient / JSON 매핑 실패 또는 output 검증 실패 시
        // 위반 사유를 포함해 재시도하거나 fallback 액션 목록으로 전환하는 흐름이 없다.
        //
        // 개선 방향:
        // - IncidentActionOutputValidator
        // - mapping failure / validation failure 구분
        // - retry prompt
        // - fallback ActionBundle
        // 을 추가한다.

        // RISK POINT:
        // LLM이 생성한 ActionItem을 RecommendedActionItem으로 1:1 변환한다.
        //
        // 아직 없는 것:
        // - restart, rollback, scale 같은 위험 키워드 필터
        // - 동일·유사 액션 중복 제거
        // - priority 범위 검증
        // - 승인 필요 여부 표시
        // - sourceSystem별 금지 액션 정책
        //
        // 예를 들어 actionTitle에 "Rollback production deployment"가 있어도
        // 현재 baseline은 사용자에게 그대로 노출한다.
        //
        // 개선 방향:
        // 수업 14에서 위험 키워드 필터, 중복 제거, risk level
        // approvalRequired 플래그를 직접 추가한다.
        List<RecommendedActionItem> items =
                output.recommendedActions().stream()
                        .map(item -> new RecommendedActionItem(
                                item.priority(),
                                item.actionTitle(),
                                item.rationale(),
                                item.ownerSuggestion()
                        ))
                        .toList();

        return new ActionBundle(items, output.responseNote());
    }

    public record ActionBundle(List<RecommendedActionItem> actions,
                               String responseNote) {
    }
}
