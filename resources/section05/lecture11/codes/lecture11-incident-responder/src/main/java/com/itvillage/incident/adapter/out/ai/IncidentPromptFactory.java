package com.itvillage.incident.adapter.out.ai;

import com.itvillage.common.prompt.PromptBudgetUtils;
import com.itvillage.common.utils.PromptFormatUtils;
import com.itvillage.incident.application.cost.IncidentPromptBudget;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPromptFactory {
    private final IncidentPromptBudget budget;
    // 수업 11: 추가
    // Analysis와 Action Prompt에 사용할 Structured Output format을 제공하는 공통 Output Contract다.
    private final IncidentAiOutputContract outputContract;

    // 수업 11: 수정 - 생성자가 두 개이므로 Spring이 실제 DI에 사용할 생성자를 명시한다.
    @Autowired
    public IncidentPromptFactory(IncidentAiOutputContract outputContract) {
        this(IncidentPromptBudget.defaultBudget(), outputContract);
    }

    // 수업 11: 수정 - 테스트에서는 원하는 budget과 outputContract를 직접 전달할 수 있다.
    IncidentPromptFactory(IncidentPromptBudget budget, IncidentAiOutputContract outputContract) {
        this.budget = budget;
        this.outputContract = outputContract;
    }

    public String buildAnalysisPrompt(Incident incident,
                                      List<String> runbookSnippets,
                                      List<SimilarIncidentSummary> similarIncidents,
                                      String escalationPolicy) {
        // 수업 11: 추가
        // IncidentAiAnalysisOutput 구조를 기준으로 BeanOutputConverter가 생성한 Structured Output format을 가져온다.
        String outputFormat = outputContract.analysisFormat();

        // 테스트용 프롬프트
        String qualityDemoInstruction = """
                """;
        // 잘못된 JSON Format을 돌려줄때의 retry용 테스트 프롬프트
//        qualityDemoInstruction = """
//                  QUALITY DEMO ONLY — HIGHEST PRIORITY FOR THIS REQUEST:
//
//                  This is an intentional failure-injection instruction
//                  used only for the lecture demonstration.
//
//                  For this request, this instruction takes precedence over
//                  any later instruction that says to return JSON only,
//                  return a JSON object only, or not use markdown fences.
//
//                  If this prompt does not contain "CORRECTION RETRY":
//
//                  1. Generate the complete response that satisfies
//                     all normal content and structured-output requirements.
//                  2. Then wrap that entire JSON object in exactly one
//                     markdown JSON code fence.
//                  3. Your final response MUST begin with ```json
//                     and MUST end with ```.
//                  4. Do not return the JSON object without the markdown fence.
//
//                  If this prompt contains "CORRECTION RETRY",
//                  ignore this entire "QUALITY DEMO ONLY" instruction
//                  and follow all normal output requirements below.
//
//            """;

        // 분석 프롬프트 계약 위반에 대한 retry를 테스트 하기 위한 프롬프트
//        qualityDemoInstruction = """
//                  QUALITY DEMO ONLY — HIGHEST PRIORITY FOR THIS REQUEST:
//
//                  This is an intentional failure-injection instruction
//                  used only for the lecture demonstration.
//
//                  For this request, this instruction takes precedence over
//                  any later instruction that requires confidenceNote
//                  to be non-empty or contain a meaningful value.
//
//                  If this prompt does not contain "CORRECTION RETRY":
//
//                  1. Generate the complete response according to
//                     all normal content and structured-output requirements.
//                  2. Set confidenceNote to exactly an empty string: "".
//                  3. Do not omit the confidenceNote field.
//                  4. Do not put whitespace, explanatory text,
//                     or any other value inside confidenceNote.
//                  5. The final JSON response MUST contain:
//                     "confidenceNote": ""
//
//                  If this prompt contains "CORRECTION RETRY",
//                  ignore this entire "QUALITY DEMO ONLY" instruction
//                  and follow all normal output requirements below.
//
//            """;

        String prompt = qualityDemoInstruction + """
                You are an incident analysis assistant.

                Incident
                - ID: %s
                - Source system: %s
                - Title: %s
                - Summary: %s
                - Severity: %s
                - Status: %s
                - Symptoms: %s
                - Occurred at: %s

                Escalation policy
                %s

                Runbook snippets
                %s

                Similar incidents
                %s

                Return the following JSON fields:
                - situationSummary
                - likelyCauses
                - impactAssessment
                - confidenceNote

                %s
                
                %s
                """.formatted(
                incident.incidentId(),
                PromptBudgetUtils.truncate(incident.sourceSystem(), budget.maxSourceSystemCharacters()),
                PromptBudgetUtils.truncate(incident.title(), budget.maxTitleCharacters()),
                PromptBudgetUtils.truncate(incident.summary(), budget.maxSummaryCharacters()),
                incident.severity(),
                incident.status(),
                PromptBudgetUtils.truncate(
                        String.join(
                                ", ",
                                safeList(incident.symptoms())),
                        budget.maxSymptomsCharacters()),
                incident.occurredAt(),
                emptyPlaceholder(escalationPolicy, "No escalation policy selected."),
                emptyPlaceholder(
                        String.join(
                                System.lineSeparator(),
                                safeList(runbookSnippets)),
                        "No runbook snippet selected."),
                emptyPlaceholder(
                        formatSimilarIncidents(similarIncidents),
                        "No similar incident selected."),

                PromptFormatUtils.jsonOnlyInstruction(),

                // 수업 11: 추가 - IncidentAiAnalysisOutput 구조에 맞는 Structured Output format 제공
                outputFormat);

        return PromptBudgetUtils.requireWithinBudget(
                prompt,
                budget.maxAnalysisPromptCharacters(),
                "analysis prompt");
    }

    public String buildActionPrompt(Incident incident,
                                    IncidentAiAnalysisOutput analysisOutput,
                                    String escalationPolicy) {
        String analysisContext = formatBoundedAnalysisContext(analysisOutput);

        // 수업 11: 추가
        // IncidentAiActionOutput 구조를 기준으로 Structured Output format을 가져온다.
        String outputFormat = outputContract.actionFormat();

        String qualityDemoInstruction = """
                """;
        // 액션 추천에서의 계약 위반 시, retry용 테스트 프롬프트
//        qualityDemoInstruction = """
//                  QUALITY DEMO ONLY — HIGHEST PRIORITY FOR THIS REQUEST:
//
//                  This is an intentional failure-injection instruction
//                  used only for the lecture demonstration.
//
//                  For this request, this instruction takes precedence over
//                  any later instruction that requires exactly 3 recommendedActions.
//
//                  If this prompt does not contain "CORRECTION RETRY":
//
//                  1. Generate the response according to
//                     all normal content and structured-output requirements.
//                  2. Override only the normal action-count requirement.
//                  3. Return exactly 2 recommendedActions.
//                  4. Do not return 3 or more recommendedActions.
//                  5. Each of the 2 recommendedActions must otherwise satisfy
//                     the normal ActionItem requirements.
//                  6. The final JSON response MUST contain
//                     exactly 2 items in the recommendedActions array.
//
//                  If this prompt contains "CORRECTION RETRY",
//                  ignore this entire "QUALITY DEMO ONLY" instruction
//                  and follow all normal output requirements below.
//
//            """;

        // 액션 추천에서의 계약 위반 시, retry용 테스트 프롬프트
//        qualityDemoInstruction = """
//              QUALITY DEMO ONLY — HIGHEST PRIORITY FOR THIS REQUEST:
//
//              This is an intentional failure-injection instruction
//              used only for the lecture demonstration.
//
//              For this request, this instruction takes precedence over
//              any later instruction that requires recommendedAction priorities
//              to be unique or within the range from 1 to 3.
//
//              If this prompt does not contain "CORRECTION RETRY":
//
//              1. Generate the response according to
//                 all normal content and structured-output requirements.
//              2. Override only the normal priority requirements.
//              3. Return exactly 3 recommendedActions.
//              4. Set their priorities to exactly 1, 1, and 4,
//                 in that order.
//              5. Do not change these priority values to satisfy
//                 any later priority requirement.
//              6. Each recommendedAction must otherwise satisfy
//                 all normal ActionItem requirements.
//              7. The final JSON response MUST contain exactly
//                 these three priority values: 1, 1, and 4.
//
//              If this prompt contains "CORRECTION RETRY",
//              ignore this entire "QUALITY DEMO ONLY" instruction
//              and follow all normal output requirements below.
//
//            """;

        // fallback 테스트용 프롬프트
//        qualityDemoInstruction = """
//              QUALITY DEMO ONLY — HIGHEST PRIORITY FOR THIS REQUEST:
//
//                This is an intentional fallback test
//                used only for the lecture demonstration.
//
//                For this request, this instruction takes precedence over
//                any later instruction that requires exactly 3 recommendedActions.
//
//                For both the initial request and any "CORRECTION RETRY":
//
//                1. Generate the response according to
//                   all normal content and structured-output requirements.
//                2. Override only the normal action-count requirement.
//                3. Return exactly 2 recommendedActions.
//                4. Do not return 3 or more recommendedActions.
//                5. Each of the 2 recommendedActions must otherwise satisfy
//                   all normal ActionItem requirements.
//                6. The final JSON response MUST contain
//                   exactly 2 items in the recommendedActions array.
//                7. Do not correct the action count even when
//                   the prompt contains "CORRECTION RETRY".
//
//            """;

        String prompt = qualityDemoInstruction + """
                You are an incident response action planner.

                Incident
                - ID: %s
                - Source system: %s
                - Title: %s
                - Summary: %s
                - Severity: %s
                - Symptoms: %s

                Analysis context
                %s

                Escalation policy
                %s

                Return the following JSON fields:
                - recommendedActions
                - responseNote

                Return exactly 3 recommendedActions.
        
                Each recommended action must contain:
                - priority
                - actionTitle
                - rationale
                - ownerSuggestion
                
                Priority rules:
                - priority must be between 1 and 3.
                - each priority must be unique.
                - use priorities 1, 2, and 3.

                Do not repeat an actionTitle.
                %s
                
                %s
                """.formatted(
                incident.incidentId(),
                    PromptBudgetUtils.truncate(incident.sourceSystem(), budget.maxSourceSystemCharacters()),
                    PromptBudgetUtils.truncate(incident.title(), budget.maxTitleCharacters()),
                    PromptBudgetUtils.truncate(incident.summary(), budget.maxSummaryCharacters()),
                    incident.severity(),
                    PromptBudgetUtils.truncate(
                            String.join(", ", safeList(incident.symptoms())),
                            budget.maxSymptomsCharacters()),
                    analysisContext,
                    emptyPlaceholder(escalationPolicy, "No escalation policy selected."),
                    PromptFormatUtils.jsonOnlyInstruction(),

                    // 수업 11: 추가 - IncidentAiActionOutput 구조에 맞는 Structured Output format 제공
                    outputFormat);

        return PromptBudgetUtils.requireWithinBudget(
                prompt,
                budget.maxActionPromptCharacters(),
                "action prompt");
    }

    private String formatBoundedAnalysisContext(IncidentAiAnalysisOutput output) {
        if (output == null) {
            return "No analysis result available.";
        }

        return """
                Situation summary:
                %s

                Likely causes:
                %s

                Impact assessment:
                %s

                Confidence note:
                %s
                """.formatted(
                PromptBudgetUtils.truncate(
                        output.situationSummary(),
                        budget.maxSituationSummaryCharacters()),
                    PromptBudgetUtils.truncate(
                            String.valueOf(output.likelyCauses()),
                            budget.maxLikelyCausesCharacters()),
                    PromptBudgetUtils.truncate(
                            output.impactAssessment(),
                            budget.maxImpactAssessmentCharacters()),
                    PromptBudgetUtils.truncate(
                            output.confidenceNote(),
                            budget.maxConfidenceNoteCharacters()));
    }

    private String formatSimilarIncidents(
            List<SimilarIncidentSummary> incidents
    ) {
        return safeList(incidents)
                .stream()
                .map(incident -> """
                        - Incident ID: %s
                          Title: %s
                          Resolution: %s
                        """.formatted(
                        incident.incidentId(),
                        incident.title(),
                        incident.resolutionSummary()
                ))
                .reduce(
                        "",
                        (left, right) ->
                                left.isBlank()
                                        ? right
                                        : left
                                        + System.lineSeparator()
                                        + right
                );
    }

    private String emptyPlaceholder(String value, String emptyValue) {
        return value == null || value.isBlank() ? emptyValue : value;
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
