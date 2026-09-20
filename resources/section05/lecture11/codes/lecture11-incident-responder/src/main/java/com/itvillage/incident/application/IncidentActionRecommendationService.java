package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.*;
import com.itvillage.incident.application.quality.IncidentActionOutputValidator;
import com.itvillage.incident.application.quality.QualityRetryFeedback;
import com.itvillage.incident.application.quality.ValidationResult;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.RecommendedActionItem;
import com.itvillage.observability.AiObservationSupport;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.itvillage.observability.IncidentObservationEvents.*;

@Service
public class IncidentActionRecommendationService {
    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;
    private final AiObservationSupport observationSupport;

    // 수업 11: 추가
    private final IncidentActionOutputValidator outputValidator;
    public IncidentActionRecommendationService(IncidentPromptFactory promptFactory,
                                               IncidentChatGateway chatGateway,
                                               IncidentActionOutputValidator outputValidator,
                                               AiObservationSupport observationSupport) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
        this.outputValidator = outputValidator;
        this.observationSupport = observationSupport;
    }

    // 수업 11: 변경
    @Observed(name = "incident.responder.action-recommendation", contextualName = "recommend-actions")
    public ActionBundle recommend(Incident incident,
                                  IncidentAnalysisResult analysisResult,
                                  String escalationPolicy) {
        try {
            var analysisOutput = new IncidentAiAnalysisOutput(
                    analysisResult.situationSummary(),
                    analysisResult.likelyCauses(),
                    analysisResult.impactAssessment(),
                    analysisResult.confidenceNote()
            );

            String prompt = promptFactory.buildActionPrompt(incident, analysisOutput, escalationPolicy);

            observationSupport.mark(ACTION_PROMPT_BUILT, incident.incidentId().toString());

            try {
                IncidentAiActionOutput output = chatGateway.generateActions(prompt);

                observationSupport.mark(ACTIONS_AI_RESPONSE_RECEIVED, incident.incidentId().toString());

                // 수업 11: 추가
                ValidationResult validation = outputValidator.validate(output);

                // 수업 11: 추가
                if (validation.valid()) {
                    return toActionBundle(output);
                }

                // 수업 11: 추가
                observationSupport.mark(ACTION_VALIDATION_FAILED, incident.incidentId().toString());

                return retryActions(incident, prompt, validation.violations()
                );
            } catch (AiOutputMappingException exception) {
                // 수업 11: 추가
                observationSupport.mark(ACTION_OUTPUT_MAPPING_FAILED, incident.incidentId().toString());

                return retryActions(incident, prompt, List.of(exception.getMessage()));
            }

        } catch (RuntimeException exception) {
            // 현재 analysis span에 error를 기록한다.
            observationSupport.recordError(exception);
            throw exception;
        }
    }

//    // 수업 7에서 사용한 코드
//    @Observed(name = "incident.responder.action-recommendation", contextualName = "recommend-actions")
//    public ActionBundle recommend(Incident incident,
//                                  IncidentAnalysisResult analysisResult,
//                                  String escalationPolicy) {
//        try {
//            var analysisOutput = new IncidentAiAnalysisOutput(
//                    analysisResult.situationSummary(),
//                    analysisResult.likelyCauses(),
//                    analysisResult.impactAssessment(),
//                    analysisResult.confidenceNote()
//            );
//
//            String prompt = promptFactory.buildActionPrompt(incident, analysisOutput, escalationPolicy);
//
//            // 수업 7: 추가
//            // 액션 Prompt 생성 완료 event
//            observationSupport.mark(ACTION_PROMPT_BUILT, incident.incidentId().toString());
//
//            IncidentAiActionOutput output = chatGateway.generateActions(prompt);
//
//            // 수업 7: 추가
//            // 모델 응답 수신과 DTO 매핑 완료 event
//            observationSupport.mark(ACTIONS_AI_RESPONSE_RECEIVED, incident.incidentId().toString());
//
//            // QUALITY POINT:
//            // JSON 매핑에 성공했더라도 recommendedActions의 개수,
//            // priority 범위와 중복, actionTitle / rationale / ownerSuggestion의 빈 값,
//            // responseNote의 존재 여부를 검증하지 않는다.
//            //
//            // 또한 ChatClient / JSON 매핑 실패 또는 output 검증 실패 시
//            // 위반 사유를 포함해 재시도하거나 fallback 액션 목록으로 전환하는 흐름이 없다.
//            //
//            // 개선 방향:
//            // - IncidentActionOutputValidator
//            // - mapping failure / validation failure 구분
//            // - retry prompt
//            // - fallback ActionBundle
//            // 을 추가한다.
//
//            // RISK POINT:
//            // LLM이 생성한 ActionItem을 RecommendedActionItem으로 1:1 변환한다.
//            //
//            // 아직 없는 것:
//            // - restart, rollback, scale 같은 위험 키워드 필터
//            // - 동일·유사 액션 중복 제거
//            // - priority 범위 검증
//            // - 승인 필요 여부 표시
//            // - sourceSystem별 금지 액션 정책
//            //
//            // 예를 들어 actionTitle에 "Rollback production deployment"가 있어도
//            // 현재 baseline은 사용자에게 그대로 노출한다.
//            //
//            // 개선 방향:
//            // 수업 14에서 위험 키워드 필터, 중복 제거, risk level
//            // approvalRequired 플래그를 직접 추가한다.
//            List<RecommendedActionItem> items =
//                    output.recommendedActions().stream()
//                            .map(item -> new RecommendedActionItem(
//                                    item.priority(),
//                                    item.actionTitle(),
//                                    item.rationale(),
//                                    item.ownerSuggestion()
//                            ))
//                            .toList();
//
//            return new ActionBundle(items, output.responseNote());
//        } catch (RuntimeException exception) {
//            // 현재 analysis span에 error를 기록한다.
//            observationSupport.recordError(exception);
//            throw exception;
//        }
//    }

    // 수업 11: 추가
    private ActionBundle retryActions(Incident incident, String initialPrompt, List<String> violations) {
        observationSupport.mark(ACTION_RETRY_STARTED, incident.incidentId().toString());

        String retryPrompt =
                initialPrompt
                        + System.lineSeparator()
                        + new QualityRetryFeedback(
                        "incident actions",
                        violations
                ).toPromptText();

        try {
            IncidentAiActionOutput retriedOutput = chatGateway.generateActions(retryPrompt);

            ValidationResult retriedValidation = outputValidator.validate(retriedOutput);

            if (retriedValidation.valid()) {
                return toActionBundle(
                        retriedOutput
                );
            }

        } catch (AiOutputMappingException exception) {
            // 두 번째 output도 mapping에 실패하면
            // 더 이상 모델을 호출하지 않고 fallback으로 간다.
        }

        observationSupport.mark(ACTION_FALLBACK_USED, incident.incidentId().toString());

        return fallbackActionBundle();
    }

    // [기존 변환 로직 유지]
    private ActionBundle toActionBundle(IncidentAiActionOutput output) {
        List<RecommendedActionItem> items =
                output
                        .recommendedActions()
                        .stream()
                        .map(item ->
                                new RecommendedActionItem(
                                        item.priority(),
                                        item.actionTitle(),
                                        item.rationale(),
                                        item.ownerSuggestion()
                                )
                        )
                        .toList();

        return new ActionBundle(items, output.responseNote());
    }

    // 수업 11: 추가
    private ActionBundle fallbackActionBundle() {
        return new ActionBundle(
                List.of(
                        new RecommendedActionItem(
                                1,
                                "현재 Incident telemetry 확인",
                                "AI 추천 결과가 기본 출력 계약을 만족하지 못했습니다.",
                                "On-call engineer"
                        ),

                        new RecommendedActionItem(
                                2,
                                "선택된 Runbook Context 검토",
                                "자동 추천 대신 이미 선택된 운영 문맥을 수동으로 확인합니다.",
                                "On-call engineer"
                        ),

                        new RecommendedActionItem(
                                3,
                                "해결되지 않으면 수동 조사로 escalation",
                                "검증되지 않은 AI 추천으로 시스템 변경을 수행하지 않습니다.",
                                "Incident owner"
                        )
                ),

                "AI 추천 검증에 실패하여 안전한 fallback 응답을 반환했습니다."
        );
    }

    public record ActionBundle(List<RecommendedActionItem> actions,
                               String responseNote) {
    }
}
