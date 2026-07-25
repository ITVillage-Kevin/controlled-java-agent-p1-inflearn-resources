package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentAnalysisService {

    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentAnalysisService(IncidentPromptFactory promptFactory, IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public IncidentAnalysisResult analyze(Incident incident,
                                          List<String> runbookSnippets,
                                          List<SimilarIncidentSummary> similarIncidents,
                                          String escalationPolicy) {
        String prompt = promptFactory.buildAnalysisPrompt(
                incident,
                runbookSnippets,
                similarIncidents,
                escalationPolicy
        );

        IncidentAiAnalysisOutput output = chatGateway.analyze(prompt);

        return new IncidentAnalysisResult(
                output.situationSummary(),
                output.likelyCauses(),
                output.impactAssessment(),
                output.confidenceNote()
        );
    }
}
