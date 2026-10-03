package com.itvillage.incident.application.quality;

import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.domain.Incident;
import org.springframework.stereotype.Component;

@Component
public class IncidentQualityEvaluator {

    private final IncidentEvaluatorPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentQualityEvaluator(IncidentEvaluatorPromptFactory promptFactory,
                                    IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public IncidentQualityEvaluation evaluateAnalysis(Incident incident,
                                                      IncidentAiAnalysisOutput analysis) {
        String prompt = promptFactory.buildAnalysisEvaluationPrompt(incident, analysis);

        return chatGateway.evaluateQuality(prompt);
    }

    public IncidentQualityEvaluation evaluateActions(Incident incident,
                                                     IncidentAiAnalysisOutput analysis,
                                                     IncidentAiActionOutput actions) {
        String prompt = promptFactory.buildActionEvaluationPrompt(incident, analysis, actions);

        return chatGateway.evaluateQuality(prompt);
    }
}
