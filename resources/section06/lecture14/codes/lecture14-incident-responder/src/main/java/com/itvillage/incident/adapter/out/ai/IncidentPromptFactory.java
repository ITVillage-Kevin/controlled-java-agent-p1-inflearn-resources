package com.itvillage.incident.adapter.out.ai;

import com.itvillage.common.prompt.PromptBudgetUtils;
import com.itvillage.common.utils.PromptFormatUtils;
import com.itvillage.incident.application.cost.IncidentPromptBudget;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.EscalationDecision;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPromptFactory {
    private final IncidentPromptBudget budget;
    private final IncidentAiOutputContract outputContract;

    @Autowired
    public IncidentPromptFactory(IncidentAiOutputContract outputContract) {
        this(IncidentPromptBudget.defaultBudget(), outputContract);
    }

    IncidentPromptFactory(IncidentPromptBudget budget, IncidentAiOutputContract outputContract) {
        this.budget = budget;
        this.outputContract = outputContract;
    }

    // 수업 14: 변경 - EscalationDecision escalationPolicy 제거
    public String buildAnalysisPrompt(Incident incident,
                                      List<String> runbookSnippets,
                                      List<SimilarIncidentSummary> similarIncidents) {
        // IncidentAiAnalysisOutput 구조를 기준으로 BeanOutputConverter가 생성한 Structured Output format을 가져온다.
        String outputFormat = outputContract.analysisFormat();

        String prompt = """
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
                emptyPlaceholder(
                        String.join(
                                System.lineSeparator(),
                                safeList(runbookSnippets)),
                        "No runbook snippet selected."),
                emptyPlaceholder(
                        formatSimilarIncidents(similarIncidents),
                        "No similar incident selected."),

                PromptFormatUtils.jsonOnlyInstruction(),

                outputFormat);

        return PromptBudgetUtils.requireWithinBudget(
                prompt,
                budget.maxAnalysisPromptCharacters(),
                "analysis prompt");
    }

    // 수업 14: 변경 - EscalationDecision escalationPolicy 제거
    public String buildActionPrompt(Incident incident,
                                    IncidentAiAnalysisOutput analysisOutput) {
        String analysisContext = formatBoundedAnalysisContext(analysisOutput);

        // IncidentAiActionOutput 구조를 기준으로 Structured Output format을 가져온다.
        String outputFormat = outputContract.actionFormat();

        String prompt = """
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
