package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.AiOutputMappingException;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.application.quality.*;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import com.itvillage.observability.AiObservationSupport;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.itvillage.observability.IncidentObservationEvents.*;

@Service
public class IncidentAnalysisService {

    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;
    private final AiObservationSupport observationSupport;
    private final IncidentAnalysisOutputValidator outputValidator;

    // 수업 12: 추가 - 기본 계약을 통과한 Analysis의 의미 품질을 통제한다.
    private final IncidentQualityControlService qualityControlService;


    public IncidentAnalysisService(IncidentPromptFactory promptFactory,
                                   IncidentChatGateway chatGateway,
                                   IncidentAnalysisOutputValidator outputValidator,
                                   AiObservationSupport observationSupport,
                                   IncidentQualityControlService qualityControlService) { // 수업 12: 추가
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
        this.outputValidator = outputValidator;
        this.observationSupport = observationSupport;
        this.qualityControlService = qualityControlService;
    }

    @Observed(name = "incident.responder.analysis", contextualName = "analyze-incident")
    public IncidentAnalysisResult analyze(Incident incident,
                                          List<String> runbookSnippets,
                                          List<SimilarIncidentSummary> similarIncidents,
                                          String escalationPolicy) {
        try {
            String prompt =
                    promptFactory.buildAnalysisPrompt(incident, runbookSnippets, similarIncidents, escalationPolicy);

            observationSupport.mark(ANALYSIS_PROMPT_BUILT, incident.incidentId().toString());

            try {
                IncidentAiAnalysisOutput output = chatGateway.analyze(prompt);

                observationSupport.mark(ANALYSIS_AI_RESPONSE_RECEIVED, incident.incidentId().toString());

                ValidationResult validation = outputValidator.validate(output);

                if (validation.valid()) {
                    // [수업 12 변경]
                    // 수업 11:
                    //
                    // return toAnalysisResult(initialOutput);
                    //
                    // 기본 계약을 통과했다고 바로 반환하지 않고
                    // 의미 품질 통제 단계로 전달한다.
                    return controlQuality(incident, output);
                }

                observationSupport.mark(ANALYSIS_VALIDATION_FAILED, incident.incidentId().toString());

                return retryAnalysis(incident, prompt, validation.violations());
            } catch (AiOutputMappingException exception) {
                observationSupport.mark(ANALYSIS_OUTPUT_MAPPING_FAILED, incident.incidentId().toString());

                return retryAnalysis(incident, prompt, List.of(exception.getMessage()));
            }

        } catch (RuntimeException exception) {
            observationSupport.recordError(exception);
            throw exception;
        }
    }

    private IncidentAnalysisResult retryAnalysis(Incident incident,
                                                 String initialPrompt,
                                                 List<String> violations) {
        observationSupport.mark(ANALYSIS_RETRY_STARTED, incident.incidentId().toString());

        String retryPrompt =
                initialPrompt
                        + System.lineSeparator()
                        + new QualityRetryFeedback("incident analysis", violations).toPromptText();

        try {
            IncidentAiAnalysisOutput retriedOutput = chatGateway.analyze(retryPrompt);

            ValidationResult retriedValidation = outputValidator.validate(retriedOutput);

            if (retriedValidation.valid()) {
                // 수업 12: 변경
                //
                // 수업 11:
                //
                // return toAnalysisResult(retriedOutput);
                //
                // retry로 기본 계약을 복구한 경우에도
                // 의미 품질 통제 단계를 거친다.
                return controlQuality(incident, retriedOutput);
            }

        } catch (AiOutputMappingException exception) {
            // 두 번째 output도 mapping에 실패하면
            // 더 이상 모델을 호출하지 않고 fallback으로 간다.
        }

        observationSupport.mark(ANALYSIS_FALLBACK_USED, incident.incidentId().toString());

        return fallbackAnalysisResult();
    }

    private IncidentAnalysisResult toAnalysisResult(IncidentAiAnalysisOutput output) {
        return new IncidentAnalysisResult(
                output.situationSummary(),
                output.likelyCauses(),
                output.impactAssessment(),
                output.confidenceNote()
        );
    }


    // 수업 12: 추가 - 수업 11의 기본 계약을 통과한 AI Analysis만이 메서드로 들어온다.
    private IncidentAnalysisResult controlQuality(Incident incident,
                                                  IncidentAiAnalysisOutput output) {
        QualityControlledResult<IncidentAiAnalysisOutput> controlledResult =
                                                qualityControlService.improveAnalysis(incident, output);

        return toAnalysisResult(controlledResult.output());
    }

    private IncidentAnalysisResult fallbackAnalysisResult() {
        return new IncidentAnalysisResult(
                "AI 분석 결과가 기본 출력 계약을 만족하지 못했습니다.",

                List.of("검증된 원인 후보가 없어 수동 조사가 필요합니다."),

                "영향 범위는 추가 확인이 필요합니다.",

                "AI 응답 검증에 실패했으므로 신뢰도는 낮습니다."
        );
    }
}
