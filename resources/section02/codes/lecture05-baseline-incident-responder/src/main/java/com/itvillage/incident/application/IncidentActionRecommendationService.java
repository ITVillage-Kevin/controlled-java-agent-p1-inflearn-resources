package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.IncidentAiActionOutput;
import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.RecommendedActionItem;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentActionRecommendationService {

    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentActionRecommendationService(IncidentPromptFactory promptFactory,
                                               IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public ActionBundle recommend(Incident incident,
                                  IncidentAnalysisResult analysisResult,
                                  String escalationPolicy) {
        var analysisOutput = new IncidentAiAnalysisOutput(
                analysisResult.situationSummary(),
                analysisResult.likelyCauses(),
                analysisResult.impactAssessment(),
                analysisResult.confidenceNote()
        );

        String prompt = promptFactory.buildActionPrompt(incident, analysisOutput, escalationPolicy);
        IncidentAiActionOutput output = chatGateway.generateActions(prompt);

        List<RecommendedActionItem> items = output.recommendedActions().stream()
                .map(it -> new RecommendedActionItem(
                        it.priority(),
                        it.actionTitle(),
                        it.rationale(),
                        it.ownerSuggestion()
                ))
                .toList();

        return new ActionBundle(items, output.responseNote());
    }

    public record ActionBundle(List<RecommendedActionItem> actions,
                               String responseNote) {
    }
}
