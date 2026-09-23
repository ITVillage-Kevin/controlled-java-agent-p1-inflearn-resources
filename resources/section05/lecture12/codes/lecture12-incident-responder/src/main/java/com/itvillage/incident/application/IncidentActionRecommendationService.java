package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.*;
import com.itvillage.incident.application.quality.*;
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
    private final IncidentActionOutputValidator outputValidator;
    private final IncidentQualityControlService qualityControlService;

    public IncidentActionRecommendationService(IncidentPromptFactory promptFactory,
                                               IncidentChatGateway chatGateway,
                                               IncidentActionOutputValidator outputValidator,
                                               AiObservationSupport observationSupport,
                                               IncidentQualityControlService qualityControlService) { // 수업 12: 추가
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
        this.outputValidator = outputValidator;
        this.observationSupport = observationSupport;
        this.qualityControlService = qualityControlService;
    }

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
                IncidentAiActionOutput actionOutput = chatGateway.generateActions(prompt);

                observationSupport.mark(ACTIONS_AI_RESPONSE_RECEIVED, incident.incidentId().toString());

                ValidationResult validation = outputValidator.validate(actionOutput);

                if (validation.valid()) {
                    // 수업 12: 변경
                    // 기본 계약을 통과했다고 바로 ActionBundle로 변환하지 않는다.
                    //
                    // 의미 품질 통제 단계로 전달한다.
                    return controlQuality(incident, analysisOutput, actionOutput);
                }

                observationSupport.mark(ACTION_VALIDATION_FAILED, incident.incidentId().toString());

                return retryActions(incident, analysisOutput, prompt, validation.violations());
            } catch (AiOutputMappingException exception) {

                observationSupport.mark(ACTION_OUTPUT_MAPPING_FAILED, incident.incidentId().toString());

                return retryActions(incident, analysisOutput, prompt, List.of(exception.getMessage()));
            }

        } catch (RuntimeException exception) {
            // 현재 analysis span에 error를 기록한다.
            observationSupport.recordError(exception);
            throw exception;
        }
    }

    private ActionBundle retryActions(Incident incident,
                                      IncidentAiAnalysisOutput analysisOutput, // 수업 12: 추가
                                      String initialPrompt,
                                      List<String> violations) {
        observationSupport.mark(ACTION_RETRY_STARTED, incident.incidentId().toString());

        String retryPrompt =
                initialPrompt
                        + System.lineSeparator()
                        + new QualityRetryFeedback(
                        "incident actions",
                        violations
                ).toPromptText();

        try {
            IncidentAiActionOutput retriedActionOutput = chatGateway.generateActions(retryPrompt);

            ValidationResult retriedValidation = outputValidator.validate(retriedActionOutput);

            if (retriedValidation.valid()) {
                // [수업 12 변경]
                // retry로 기본 계약을 복구한 Action도
                // 의미 품질 통제를 거친다.
                return controlQuality(incident, analysisOutput, retriedActionOutput);
            }

        } catch (AiOutputMappingException exception) {
            // 두 번째 output도 mapping에 실패하면
            // 더 이상 모델을 호출하지 않고 fallback으로 간다.
        }

        observationSupport.mark(ACTION_FALLBACK_USED, incident.incidentId().toString());

        return fallbackActionBundle();
    }

    // 수업 12: 추가
    private ActionBundle controlQuality(Incident incident,
                                        IncidentAiAnalysisOutput analysis,
                                        IncidentAiActionOutput actions) {
        QualityControlledResult<IncidentAiActionOutput> controlledResult =
                                            qualityControlService.improveActions(incident, analysis, actions);

        return toActionBundle(controlledResult.output());
    }

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
