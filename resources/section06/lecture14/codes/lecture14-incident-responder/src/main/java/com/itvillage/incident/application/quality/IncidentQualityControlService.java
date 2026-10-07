package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.EscalationDecision;
import com.itvillage.observability.AiObservationSupport;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

import static com.itvillage.observability.IncidentObservationEvents.*;

@Service
public class IncidentQualityControlService {
    private final IncidentQualityEvaluator evaluator;
    private final IncidentQualityOptimizer optimizer;
    private final IncidentAnalysisOutputValidator analysisValidator;
    private final IncidentActionOutputValidator actionValidator;
    private final AiObservationSupport observationSupport;

    public IncidentQualityControlService(IncidentQualityEvaluator evaluator,
                                         IncidentQualityOptimizer optimizer,
                                         IncidentAnalysisOutputValidator analysisValidator,
                                         IncidentActionOutputValidator actionValidator,
                                         AiObservationSupport observationSupport) {
        this.evaluator = evaluator;
        this.optimizer = optimizer;
        this.analysisValidator = analysisValidator;
        this.actionValidator = actionValidator;
        this.observationSupport = observationSupport;
    }

    @Observed(name = "incident.quality.analysis", contextualName = "control-analysis-quality")
    public QualityControlledResult<IncidentAiAnalysisOutput> improveAnalysis(Incident incident,
                                                                             IncidentAiAnalysisOutput initialAnalysis,
                                                                             EscalationDecision escalationPolicy) { // 수업 14: 변경
        try {
            // Evaluator로 평가
            IncidentQualityEvaluation initialEvaluation =
                    evaluator.evaluateAnalysis(incident, initialAnalysis, escalationPolicy); // 수업 14: 변경

            if (initialEvaluation.passed()) {
                observationSupport.mark(ANALYSIS_QUALITY_PASSED, incident.incidentId().toString());

                return QualityControlledResult.passed(initialAnalysis);
            }

            observationSupport.mark(ANALYSIS_QUALITY_NEEDS_REVISION, incident.incidentId().toString());
            observationSupport.mark(ANALYSIS_OPTIMIZATION_STARTED, incident.incidentId().toString());

            // Optimizer로 개선
            IncidentAiAnalysisOutput optimizedAnalysis =
                optimizer.optimizeAnalysis(incident, initialAnalysis, initialEvaluation, escalationPolicy); // 수업 14: 변경

            ValidationResult validationResult = analysisValidator.validate(optimizedAnalysis);

            if (!validationResult.valid()) {
                observationSupport.mark(
                        ANALYSIS_QUALITY_NOT_RESOLVED, incident.incidentId().toString());

                return QualityControlledResult.notResolved(
                        initialAnalysis
                );
            }

            // Evaluator로 재평가
            IncidentQualityEvaluation reEvaluation =
                    evaluator.evaluateAnalysis(incident, optimizedAnalysis, escalationPolicy); // 수업 14: 변경

            // PASS
            if (reEvaluation.passed()) {
                observationSupport.mark(ANALYSIS_RE_EVALUATION_PASSED, incident.incidentId().toString());
                observationSupport.mark(ANALYSIS_QUALITY_IMPROVED, incident.incidentId().toString());

                return QualityControlledResult.improved(optimizedAnalysis);
            }

            observationSupport.mark(ANALYSIS_QUALITY_NOT_RESOLVED, incident.incidentId().toString());

            // NOT RESOLVED
            return QualityControlledResult.notResolved(initialAnalysis);
        } catch (RuntimeException exception) {
            observationSupport.recordError(exception);
            throw exception;
        }
    }

    @Observed(name = "incident.quality.actions", contextualName = "control-action-quality")
    public QualityControlledResult<IncidentAiActionOutput> improveActions(Incident incident,
                                                                          IncidentAiAnalysisOutput analysis,
                                                                          IncidentAiActionOutput initialActions,
                                                                          EscalationDecision escalationPolicy) { // 수업 14: 변경
        try {
            IncidentQualityEvaluation initialEvaluation =
                    evaluator.evaluateActions(incident, analysis, initialActions, escalationPolicy);

            if (initialEvaluation.passed()) {
                observationSupport.mark(ACTION_QUALITY_PASSED, incident.incidentId().toString());

                return QualityControlledResult.passed(initialActions);
            }

            observationSupport.mark(ACTION_QUALITY_NEEDS_REVISION, incident.incidentId().toString());
            observationSupport.mark(ACTION_OPTIMIZATION_STARTED, incident.incidentId().toString());

            IncidentAiActionOutput optimizedActions =
                    optimizer.optimizeActions(
                            incident, analysis, initialActions, initialEvaluation, escalationPolicy); // 수업 14: 변경

            ValidationResult validationResult = actionValidator.validate(optimizedActions);

            if (!validationResult.valid()) {
                observationSupport.mark(ACTION_QUALITY_NOT_RESOLVED, incident.incidentId().toString());

                return QualityControlledResult.notResolved(initialActions);
            }

            IncidentQualityEvaluation reEvaluation =
                    evaluator.evaluateActions(incident, analysis, optimizedActions, escalationPolicy); // 수업 14: 변경

            if (reEvaluation.passed()) {
                observationSupport.mark(ACTION_RE_EVALUATION_PASSED, incident.incidentId().toString());
                observationSupport.mark(ACTION_QUALITY_IMPROVED, incident.incidentId().toString());

                return QualityControlledResult.improved(optimizedActions);
            }

            observationSupport.mark(ACTION_QUALITY_NOT_RESOLVED, incident.incidentId().toString());

            return QualityControlledResult.notResolved(initialActions);
        } catch (RuntimeException exception) {
            observationSupport.recordError(exception);
            throw exception;
        }
    }
}
