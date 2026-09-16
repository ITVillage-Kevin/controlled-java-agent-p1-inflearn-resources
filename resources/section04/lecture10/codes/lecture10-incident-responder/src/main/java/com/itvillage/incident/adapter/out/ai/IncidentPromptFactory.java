package com.itvillage.incident.adapter.out.ai;

import com.itvillage.common.prompt.PromptBudgetUtils;
import com.itvillage.common.utils.PromptFormatUtils;
import com.itvillage.incident.application.cost.IncidentPromptBudget;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPromptFactory {
    // 수업 10: 추가
    private final IncidentPromptBudget budget;

    public IncidentPromptFactory() {
        this(IncidentPromptBudget.defaultBudget());
    }

    IncidentPromptFactory(IncidentPromptBudget budget) {
        this.budget = budget;
    }

    public String buildAnalysisPrompt(Incident incident,
                                      List<String> runbookSnippets,
                                      List<SimilarIncidentSummary> similarIncidents,
                                      String escalationPolicy) {
        // COST POINT:
        // - 전달할 Runbook과 과거 Incident Context를 개수 제한없이 그대로 Prompt에 포함한다.
        // - 또한 외부에서 들어온 Incident의 title, summary, symptoms도 크기 상한 없이 그대로 Prompt에 들어간다.
        //
        // 따라서 데이터와 입력이 커질수록 LLM에 전달되는 Context도 함께 증가할 수 있다.
        //
        // 개선 방향:
        // - 관련 Context의 중복을 줄이고
        // - 중요도와 Incident 특성에 따라 전달 개수에 상한을 두고
        // - 크기를 신뢰하기 어려운 외부 입력에는 명시적인 제한을 둔다.
        //
        // 어떤 Context를 몇 개까지 허용할지,
        // 어떤 입력의 크기를 제한할지는
        // 실제 데이터 구조와 실행 결과를 관찰한 뒤 결정한다.
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

                PromptFormatUtils.jsonOnlyInstruction()
        );

        return PromptBudgetUtils.requireWithinBudget(
                prompt,
                budget.maxAnalysisPromptCharacters(),
                "analysis prompt");
//        return """
//                You are an incident analysis assistant for Korean operations teams.
//
//                Analyze the incident and produce a concise operational analysis.
//
//                Language rules:
//                - Write every human-readable descriptive field value in Korean.
//                - Use concise, natural, operational Korean suitable for real incident handling.
//                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
//                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
//                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
//                - If an English technical term is commonly used in practice, you may keep the term,
//                  but the surrounding explanation must still be written in Korean.
//
//                Writing rules:
//                - Ground the analysis in the provided incident data, runbook snippets, similar incidents, and escalation policy.
//                - Do not invent unsupported facts.
//                - If uncertainty exists, state it clearly in Korean.
//                - Keep the response short, practical, and operational.
//                - likelyCauses must contain 2 to 4 short Korean items.
//
//                Incident:
//                - id: %s
//                - sourceSystem: %s
//                - title: %s
//                - summary: %s
//                - severity: %s
//                - status: %s
//                - symptoms: %s
//                - occurredAt: %s
//
//                Escalation policy:
//                %s
//
//                Runbook snippets:
//                %s
//
//                Similar incidents:
//                %s
//
//                Return fields:
//                - situationSummary (Korean)
//                - likelyCauses (array, 2~4 items, Korean)
//                - impactAssessment (Korean)
//                - confidenceNote (Korean)
//
//                Important:
//                - All descriptive response values must be written in Korean.
//                - Do not translate identifiers or enum-like control values unnecessarily.
//                - Return JSON only.
//
//                %s
//                """.formatted(
//                incident.incidentId(),
//                incident.sourceSystem(),
//                incident.title(),
//                incident.summary(),
//                incident.severity(),
//                incident.status(),
//                incident.symptoms(),
//                incident.occurredAt(),
//                escalationPolicy,
//                runbookSnippets,
//                similarIncidents,
//                PromptFormatUtils.jsonOnlyInstruction()
//        );
    }

    public String buildActionPrompt(Incident incident,
                                    IncidentAiAnalysisOutput analysisOutput,
                                    String escalationPolicy) {
        String analysisContext = formatBoundedAnalysisContext(analysisOutput);

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

                Escalation policy
                %s

                Return the following JSON fields:
                - recommendedActions
                - responseNote

                Each recommended action must contain:
                - priority
                - actionTitle
                - rationale
                - ownerSuggestion

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
                    PromptFormatUtils.jsonOnlyInstruction());

        return PromptBudgetUtils.requireWithinBudget(
                prompt,
                budget.maxActionPromptCharacters(),
                "action prompt");

        // COST POINT:
        // - 첫 번째 LLM이 생성한 분석 결과를 두 번째 액션 Prompt에 그대로 다시 포함한다.
        //
        // 첫 번째 LLM의 output은 두 번째 LLM 호출에서는 다시 input이 되기 때문에,
        // 분석 결과가 길어질수록 다음 호출의 입력 크기도 증가할 수 있다.
        //
        // 개선 방향:
        // - 액션 생성에 필요한 분석 정보는 유지하되,
        // 이전 LLM 출력이 다음 Prompt에 무제한으로 재주입되지 않도록 명시적인 크기 상한을 둔다.
        //
        // - 여러 번 LLM을 호출하는 흐름에서는 사용자 입력과 검색 Context뿐 아니라
        // 이전 LLM 결과의 재주입 크기도 비용 통제 지점으로 본다.
//        return """
//                You are an incident response action planner for Korean operations teams.
//
//                Based on the incident and analysis, propose practical immediate actions.
//
//                Language rules:
//                - Write every human-readable descriptive field value in Korean.
//                - Use concise, actionable Korean suitable for on-call engineers and incident coordinators.
//                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
//                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
//                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
//                - If an English technical term is commonly used in practice, you may keep the term,
//                  but the surrounding explanation must still be written in Korean.
//
//                Writing rules:
//                - Recommend practical actions that can be executed immediately.
//                - Prioritize actions by urgency and operational value.
//                - Avoid vague recommendations.
//                - Do not invent unsupported facts.
//                - responseNote must be concise and written in Korean.
//
//                Incident:
//                - id: %s
//                - sourceSystem: %s
//                - title: %s
//                - summary: %s
//                - severity: %s
//                - symptoms: %s
//
//                Analysis:
//                - situationSummary: %s
//                - likelyCauses: %s
//                - impactAssessment: %s
//                - confidenceNote: %s
//
//                Escalation policy:
//                %s
//
//                Return JSON with:
//                - recommendedActions: array of 3 items
//                  - priority
//                  - actionTitle (Korean)
//                  - rationale (Korean)
//                  - ownerSuggestion (Korean where natural)
//                - responseNote (Korean)
//
//                Important:
//                - All descriptive response values must be written in Korean.
//                - Do not translate identifiers or enum-like control values unnecessarily.
//                - Return JSON only.
//
//                %s
//                """.formatted(
//                incident.incidentId(),
//                incident.sourceSystem(),
//                incident.title(),
//                incident.summary(),
//                incident.severity(),
//                incident.symptoms(),
//                analysisOutput.situationSummary(),
//                analysisOutput.likelyCauses(),
//                analysisOutput.impactAssessment(),
//                analysisOutput.confidenceNote(),
//                escalationPolicy,
//                PromptFormatUtils.jsonOnlyInstruction()
//        );
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

    private String emptyPlaceholder(
            String value,
            String emptyValue
    ) {
        return value == null
                || value.isBlank()
                ? emptyValue
                : value;
    }

    private <T> List<T> safeList(
            List<T> values
    ) {
        return values == null
                ? List.of()
                : values;
    }
}
