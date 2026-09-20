package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.AiOutputMappingException;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.application.quality.IncidentAnalysisOutputValidator;
import com.itvillage.incident.application.quality.QualityRetryFeedback;
import com.itvillage.incident.application.quality.ValidationResult;
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

    // 수업 11: 추가
    // 구조화된 Analysis output이 애플리케이션 기본 계약을 만족하는지 검사한다.
    private final IncidentAnalysisOutputValidator outputValidator;

    public IncidentAnalysisService(IncidentPromptFactory promptFactory,
                                   IncidentChatGateway chatGateway,
                                   IncidentAnalysisOutputValidator outputValidator, // 수업 11: 추가
                                   AiObservationSupport observationSupport) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
        this.outputValidator = outputValidator;
        this.observationSupport = observationSupport;
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

                // 수업 11: 추가
                // 매핑 성공 후 바로 사용하지 않고 기본 출력 계약을 검사한다.
                ValidationResult validation = outputValidator.validate(output);

                // 수업 11: 추가
                if (validation.valid()) {
                    return toAnalysisResult(output);
                }

                // 수업 11: 추가
                observationSupport.mark(ANALYSIS_VALIDATION_FAILED, incident.incidentId().toString());

                // 수업 11: 변경
                return retryAnalysis(incident, prompt, validation.violations());
            } catch (AiOutputMappingException exception) {
                // 수업 11: 변경
                // Provider failure가 아니라 AI output mapping failure인 경우에만 품질 retry 흐름으로 들어간다.
                observationSupport.mark(ANALYSIS_OUTPUT_MAPPING_FAILED, incident.incidentId().toString());

                return retryAnalysis(incident, prompt, List.of(exception.getMessage()));
            }

        } catch (RuntimeException exception) {
            // 품질 retry로 복구되지 않은 RuntimeException을
            // 현재 analysis span에 error로 기록한다.
            observationSupport.recordError(exception);
            throw exception;
        }
    }

    // 수업 10까지의 코드
//    @Observed(name = "incident.responder.analysis", contextualName = "analyze-incident")
//    public IncidentAnalysisResult analyze(Incident incident,
//                                          List<String> runbookSnippets,
//                                          List<SimilarIncidentSummary> similarIncidents,
//                                          String escalationPolicy) {
//        try {
//            String prompt = promptFactory.buildAnalysisPrompt(
//                    incident,
//                    runbookSnippets,
//                    similarIncidents,
//                    escalationPolicy
//            );
//
//            observationSupport.mark(ANALYSIS_PROMPT_BUILT, incident.incidentId().toString());
//
//            IncidentAiAnalysisOutput output = chatGateway.analyze(prompt);
//
//            observationSupport.mark(ANALYSIS_AI_RESPONSE_RECEIVED, incident.incidentId().toString());
//
//            // QUALITY POINT:
//            // JSON 매핑에 성공했더라도 output의 필수 필드, 빈 값,
//            // likelyCauses 목록 개수, 기본 비즈니스 제약을 검증하지 않는다.
//            //
//            // 또한 ChatClient / JSON 매핑 실패 또는 output 검증 실패 시
//            // 위반 사유를 포함해 재시도하거나 fallback 결과로 전환하는 흐름이 없다.
//            //
//            // 개선 방향:
//            // - IncidentAnalysisOutputValidator
//            // - mapping failure / validation failure 구분
//            // - retry prompt
//            // - fallback IncidentAnalysisResult
//            // 를 추가한다.
//            return new IncidentAnalysisResult(
//                    output.situationSummary(),
//                    output.likelyCauses(),
//                    output.impactAssessment(),
//                    output.confidenceNote()
//            );
//        } catch (RuntimeException exception) {
//            // 현재 analysis span에 error를 기록한다.
//            observationSupport.recordError(exception);
//            throw exception;
//        }
//    }

    // 수업 11: 추가
    // 품질 실패 시 단 한 번만 수행하는 retry 흐름이다.
    private IncidentAnalysisResult retryAnalysis(Incident incident,
                                                 String initialPrompt,
                                                 List<String> violations) {
        observationSupport.mark(ANALYSIS_RETRY_STARTED, incident.incidentId().toString());

        // 같은 Prompt를 그대로 다시 보내지 않는다.
        // 실패 이유를 correction feedback으로 추가한다.
        String retryPrompt =
                initialPrompt
                        + System.lineSeparator()
                        + new QualityRetryFeedback("incident analysis", violations).toPromptText();

        try {
            IncidentAiAnalysisOutput retriedOutput = chatGateway.analyze(retryPrompt);

            // retry 결과도 동일한 Validator로 다시 검사한다.
            ValidationResult retriedValidation = outputValidator.validate(retriedOutput);

            if (retriedValidation.valid()) {
                return toAnalysisResult(retriedOutput);
            }

        } catch (AiOutputMappingException exception) {
            // 두 번째 output도 mapping에 실패하면
            // 더 이상 모델을 호출하지 않고 fallback으로 간다.
        }

        // [수업 11 변경]
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

    // 수업 11: 추가
    // LLM 재호출 없이 Java 코드가 만드는 deterministic fallback이다.
    private IncidentAnalysisResult fallbackAnalysisResult() {
        return new IncidentAnalysisResult(
                "AI 분석 결과가 기본 출력 계약을 만족하지 못했습니다.",

                List.of("검증된 원인 후보가 없어 수동 조사가 필요합니다."),

                "영향 범위는 추가 확인이 필요합니다.",

                "AI 응답 검증에 실패했으므로 신뢰도는 낮습니다."
        );
    }
}
