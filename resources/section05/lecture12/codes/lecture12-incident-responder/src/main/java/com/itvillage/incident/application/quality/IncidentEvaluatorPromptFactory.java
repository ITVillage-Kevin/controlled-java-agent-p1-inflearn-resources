package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiOutputContract;
import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.domain.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentEvaluatorPromptFactory {

    private final IncidentAiOutputContract outputContract;

    public IncidentEvaluatorPromptFactory(IncidentAiOutputContract outputContract) {
        this.outputContract = outputContract;
    }

    public String buildAnalysisEvaluationPrompt(Incident incident,
                                                IncidentAiAnalysisOutput analysisOutput) {
        return """
                You are an incident response quality evaluator.

                Evaluate the analysis against
                the provided incident context.

                Do not rewrite the analysis.
                Evaluate it only.

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

                ANALYSIS TO EVALUATE

                Situation summary:
                %s

                Likely causes:
                %s

                Impact assessment:
                %s

                Confidence note:
                %s

                EVALUATION PRINCIPLES
                
                Evaluate whether the analysis meets
                the minimum acceptable semantic-quality
                requirements for this incident.
                
                Do not require an ideal or exhaustive analysis.
                
                Evaluate specificity only within
                the available incident evidence.
                
                Do not require details that are not present
                in the provided incident context.
                
                Do not request invented SQL statements,
                code changes, configuration values,
                business metrics, or customer complaints.
                
                A likely cause may be expressed as a hypothesis
                when the evidence does not establish
                a confirmed root cause.
                
                Do not reject an analysis merely because
                additional details or examples could be added.
                
                EVALUATION CRITERIA
                
                1. Evidence alignment
                
                   Likely causes must be reasonably connected
                   to the observed incident symptoms
                   or other provided incident evidence.
                
                   Reject causes that contradict the evidence
                   or introduce unsupported factual claims.
                
                2. Specificity
                
                   Likely causes must identify plausible
                   technical areas or mechanisms
                   that can guide further investigation.
                
                   Generic statements such as
                   "There may be a system issue"
                   are insufficient.
                
                   However, do not require exact SQL statements,
                   code changes, configuration values,
                   or other details that are unavailable.
                
                   A clearly stated, evidence-grounded hypothesis
                   is acceptable.
                
                3. Impact quality
                
                   The impact assessment must explain
                   practical user or business impact
                   reasonably supported by the incident.
                
                   Concrete effects such as increased latency,
                   failed requests, or reduced availability
                   are acceptable when relevant.
                
                   Do not require numerical business impact,
                   revenue loss, or customer complaints
                   unless supported by the provided evidence.
                
                4. Confidence calibration
                
                   Confidence must reflect the strength
                   and limitations of the available evidence.
                
                   An analysis may express uncertainty
                   while still identifying plausible causes.
                
                   Do not require a confirmed root cause
                   when the evidence is insufficient.
                
                5. Unsupported certainty
                
                   Reject an unverified root cause
                   presented as an established fact.
                
                   Do not reject a plausible hypothesis
                   merely because it has not yet been confirmed,
                   provided that its uncertainty is clear.
                
                DECISION RULES
                
                Return PASS when the analysis satisfies
                all five evaluation criteria
                at a practically acceptable level.
                
                Return NEEDS_REVISION only when
                one or more substantive quality problems
                remain under the criteria above.
                
                A substantive quality problem is one
                that materially reduces the analysis's
                evidence alignment, investigative usefulness,
                impact explanation, or confidence calibration.
                
                Do not return NEEDS_REVISION solely because
                the analysis could be more detailed,
                more polished, or more comprehensive.
                
                When decision is PASS:
                - issues must be an empty array.
                - revisionInstructions must be an empty array.
                
                When decision is NEEDS_REVISION:
                - issues must identify the substantive
                  quality problems.
                - revisionInstructions must describe
                  corrections that can be made using
                  the available incident evidence.
                - Do not request information that is absent
                  from the provided incident context.

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
                analysisOutput.situationSummary(),
                analysisOutput.likelyCauses(),
                analysisOutput.impactAssessment(),
                analysisOutput.confidenceNote(),
                outputContract.qualityEvaluationFormat()
        );
    }

    public String buildActionEvaluationPrompt(Incident incident,
                                              IncidentAiAnalysisOutput analysisOutput,
                                              IncidentAiActionOutput actionsOutput) {
        return """
                You are an incident response action quality evaluator.

                Evaluate the recommended actions against
                the incident and analysis context.

                Do not rewrite the actions.
                Evaluate them only.

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

                ACTIONS TO EVALUATE

                %s

                EVALUATION PRINCIPLES
                
                Evaluate whether the recommended actions
                meet the minimum acceptable semantic-quality
                requirements for this incident.
                
                Do not require an ideal or exhaustive
                incident response plan.
                
                Evaluate the actions using only
                the provided incident and analysis context.
                
                Do not require unavailable logs,
                metrics, SQL statements, configuration values,
                or code change details.
                
                Do not reject an action merely because
                it could include additional implementation
                details or alternative investigation steps.
                
                EVALUATION CRITERIA
                
                1. Incident relevance
                
                   Each action must address
                   the current incident symptoms,
                   a plausible cause,
                   or an appropriate investigation step.
                
                   Reject actions that are unrelated
                   to the current incident.
                
                2. Analysis alignment
                
                   Actions and rationales must be reasonably
                   connected to the current incident
                   and analysis context.
                
                   An action may investigate an unconfirmed
                   cause when the uncertainty is clear.
                
                   Do not treat an unconfirmed hypothesis
                   as an established root cause.
                
                3. Action specificity
                
                   Each action must identify
                   a concrete investigation or verification
                   activity that an operator can understand.
                
                   Generic actions such as
                   "Investigate the issue"
                   are insufficient.
                
                   However, do not require exact commands,
                   SQL statements, configuration values,
                   or execution procedures
                   that are unavailable in the context.
                
                4. Rationale quality
                
                   Each rationale must explain
                   why the action is relevant
                   to the current incident.
                
                   The explanation must connect
                   the proposed action to the observed symptoms,
                   available evidence, or plausible causes.
                
                5. Priority quality
                
                   The action order must be reasonable
                   for the current incident context.
                
                   Reject a priority order only when
                   it creates a substantive problem
                   in the investigation sequence.
                
                   Do not require a different order
                   merely because another reasonable
                   investigation sequence is possible.
                
                Do not evaluate execution permission,
                approval requirements,
                or whether an action may actually
                change an external system.
                
                DECISION RULES
                
                Return PASS when all five evaluation criteria
                are satisfied at a practically acceptable level.
                
                Return NEEDS_REVISION only when
                one or more substantive quality problems
                remain under the criteria above.
                
                A substantive quality problem is one
                that materially reduces incident relevance,
                investigative usefulness, rationale quality,
                or the appropriateness of the action order.
                
                Do not return NEEDS_REVISION solely because
                an action could be more detailed,
                more polished, or more comprehensive.
                
                When decision is PASS:
                - issues must be an empty array.
                - revisionInstructions must be an empty array.
                
                When decision is NEEDS_REVISION:
                - issues must identify the substantive
                  quality problems.
                - revisionInstructions must describe
                  corrections supported by the provided
                  incident and analysis context.
                - Do not request unavailable information.
                
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
                analysisOutput.situationSummary(),
                analysisOutput.likelyCauses(),
                analysisOutput.impactAssessment(),
                analysisOutput.confidenceNote(),
                actionsOutput,
                outputContract.qualityEvaluationFormat()
        );
    }
}
