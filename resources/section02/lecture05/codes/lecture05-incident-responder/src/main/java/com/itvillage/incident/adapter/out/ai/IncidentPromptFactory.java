package com.itvillage.incident.adapter.out.ai;

import com.itvillage.common.utils.PromptFormatUtils;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPromptFactory {

    public String buildAnalysisPrompt(
            Incident incident,
            List<String> runbookSnippets,
            List<SimilarIncidentSummary> similarIncidents,
            String escalationPolicy
    ) {
        return """
                You are an incident analysis assistant for Korean operations teams.

                Analyze the incident and produce a concise operational analysis.

                Language rules:
                - Write every human-readable descriptive field value in Korean.
                - Use concise, natural, operational Korean suitable for real incident handling.
                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
                - If an English technical term is commonly used in practice, you may keep the term,
                  but the surrounding explanation must still be written in Korean.

                Writing rules:
                - Ground the analysis in the provided incident data, runbook snippets, similar incidents, and escalation policy.
                - Do not invent unsupported facts.
                - If uncertainty exists, state it clearly in Korean.
                - Keep the response short, practical, and operational.
                - likelyCauses must contain 2 to 4 short Korean items.

                Incident:
                - id: %s
                - sourceSystem: %s
                - title: %s
                - summary: %s
                - severity: %s
                - status: %s
                - symptoms: %s
                - occurredAt: %s

                Escalation policy:
                %s

                Runbook snippets:
                %s

                Similar incidents:
                %s

                Return fields:
                - situationSummary (Korean)
                - likelyCauses (array, 2~4 items, Korean)
                - impactAssessment (Korean)
                - confidenceNote (Korean)

                Important:
                - All descriptive response values must be written in Korean.
                - Do not translate identifiers or enum-like control values unnecessarily.
                - Return JSON only.

                %s
                """.formatted(
                incident.incidentId(),
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.status(),
                incident.symptoms(),
                incident.occurredAt(),
                escalationPolicy,
                runbookSnippets,
                similarIncidents,
                PromptFormatUtils.jsonOnlyInstruction()
        );
    }

    public String buildActionPrompt(
            Incident incident,
            IncidentAiAnalysisOutput analysisOutput,
            String escalationPolicy
    ) {
        return """
                You are an incident response action planner for Korean operations teams.

                Based on the incident and analysis, propose practical immediate actions.

                Language rules:
                - Write every human-readable descriptive field value in Korean.
                - Use concise, actionable Korean suitable for on-call engineers and incident coordinators.
                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
                - If an English technical term is commonly used in practice, you may keep the term,
                  but the surrounding explanation must still be written in Korean.

                Writing rules:
                - Recommend practical actions that can be executed immediately.
                - Prioritize actions by urgency and operational value.
                - Avoid vague recommendations.
                - Do not invent unsupported facts.
                - responseNote must be concise and written in Korean.

                Incident:
                - id: %s
                - sourceSystem: %s
                - title: %s
                - summary: %s
                - severity: %s
                - symptoms: %s

                Analysis:
                - situationSummary: %s
                - likelyCauses: %s
                - impactAssessment: %s
                - confidenceNote: %s

                Escalation policy:
                %s

                Return JSON with:
                - recommendedActions: array of 3 items
                  - priority
                  - actionTitle (Korean)
                  - rationale (Korean)
                  - ownerSuggestion (Korean where natural)
                - responseNote (Korean)

                Important:
                - All descriptive response values must be written in Korean.
                - Do not translate identifiers or enum-like control values unnecessarily.
                - Return JSON only.

                %s
                """.formatted(
                incident.incidentId(),
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.symptoms(),
                analysisOutput.situationSummary(),
                analysisOutput.likelyCauses(),
                analysisOutput.impactAssessment(),
                analysisOutput.confidenceNote(),
                escalationPolicy,
                PromptFormatUtils.jsonOnlyInstruction()
        );
    }
}
