package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.domain.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentQualityOptimizer {

    private final IncidentOptimizerPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentQualityOptimizer(IncidentOptimizerPromptFactory promptFactory,
                                    IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public IncidentAiAnalysisOutput optimizeAnalysis(Incident incident,
                                                     IncidentAiAnalysisOutput currentAnalysis,
                                                     IncidentQualityEvaluation evaluation) {
        String prompt = promptFactory.buildAnalysisOptimizationPrompt(incident, currentAnalysis, evaluation);

        return chatGateway.optimizeAnalysis(prompt);
    }

    public IncidentAiActionOutput optimizeActions(Incident incident,
                                                  IncidentAiAnalysisOutput analysis,
                                                  IncidentAiActionOutput currentActions,
                                                  IncidentQualityEvaluation evaluation) {
        String prompt = promptFactory.buildActionOptimizationPrompt(incident, analysis, currentActions, evaluation);

        return chatGateway.optimizeActions(prompt);
    }
}
