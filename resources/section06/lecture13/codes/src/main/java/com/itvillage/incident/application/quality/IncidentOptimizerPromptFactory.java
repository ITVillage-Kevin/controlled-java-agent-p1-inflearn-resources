package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiOutputContract;
import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.domain.Incident;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class IncidentOptimizerPromptFactory {

    private final IncidentAiOutputContract outputContract;

    public IncidentOptimizerPromptFactory(IncidentAiOutputContract outputContract) {
        this.outputContract = outputContract;
    }

    public String buildAnalysisOptimizationPrompt(Incident incident,
                                                  IncidentAiAnalysisOutput currentAnalysis,
                                                  IncidentQualityEvaluation evaluation) {
        return """
                You are an incident response analysis optimizer.

                Revise the current analysis using
                the evaluator feedback.

                Do not invent new incident facts.
                Do not claim evidence that is not present
                in the provided incident context.

                INCIDENT CONTEXT

                Source system:
                %s

                Title:
                %s

                Summary:
                %s

                Severity:
                %s

                Symptoms:
                %s

                CURRENT ANALYSIS

                Situation summary:
                %s

                Likely causes:
                %s

                Impact assessment:
                %s

                Confidence note:
                %s

                EVALUATOR ISSUES

                %s

                REVISION INSTRUCTIONS

                %s

                Address the evaluator feedback.

                Preserve valid information when possible.
                Change only what is needed.

                Return JSON only.
                Do not include markdown fences.
                Do not add explanations before or after JSON.

                %s
                """.formatted(
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.symptoms(),
                currentAnalysis.situationSummary(),
                currentAnalysis.likelyCauses(),
                currentAnalysis.impactAssessment(),
                currentAnalysis.confidenceNote(),
                formatList(evaluation.issues()),
                formatList(evaluation.revisionInstructions()),
                outputContract.analysisFormat()
        );
    }

    public String buildActionOptimizationPrompt(Incident incident,
                                                IncidentAiAnalysisOutput analysis,
                                                IncidentAiActionOutput currentActions,
                                                IncidentQualityEvaluation evaluation) {
        return """
                You are an incident response action optimizer.

                Revise the recommended actions using
                the evaluator feedback.

                Do not invent new incident facts.

                Do not add execution permission,
                approval decisions,
                or claims that an external system
                has actually been changed.

                INCIDENT CONTEXT

                Source system:
                %s

                Title:
                %s

                Summary:
                %s

                Severity:
                %s

                Symptoms:
                %s

                ANALYSIS CONTEXT

                Situation summary:
                %s

                Likely causes:
                %s

                Impact assessment:
                %s

                Confidence note:
                %s

                CURRENT ACTIONS

                %s

                EVALUATOR ISSUES

                %s

                REVISION INSTRUCTIONS

                %s

                Address the evaluator feedback.

                Preserve valid actions when possible.
                Change only what is needed.

                Return exactly 3 recommendedActions.
                Use unique priorities 1, 2, and 3.

                Return JSON only.
                Do not include markdown fences.
                Do not add explanations before or after JSON.

                %s
                """.formatted(
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.symptoms(),
                analysis.situationSummary(),
                analysis.likelyCauses(),
                analysis.impactAssessment(),
                analysis.confidenceNote(),
                currentActions,
                formatList(evaluation.issues()),
                formatList(evaluation.revisionInstructions()),
                outputContract.actionFormat()
        );
    }

    private String formatList(List<String> values) {
        return values.stream()
                .map(value -> "- " + value)
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
