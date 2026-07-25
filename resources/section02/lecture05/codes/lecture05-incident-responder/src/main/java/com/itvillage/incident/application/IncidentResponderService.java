package com.itvillage.incident.application;

import com.itvillage.common.exception.IncidentException;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.domain.IncidentStatus;
import com.itvillage.incident.dto.IncidentRequest;
import com.itvillage.incident.dto.IncidentResponse;
import com.itvillage.incident.port.in.RespondToIncidentUseCase;
import com.itvillage.incident.port.out.AlertPolicyPort;
import com.itvillage.incident.port.out.IncidentHistoryPort;
import com.itvillage.incident.port.out.IncidentKnowledgePort;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class IncidentResponderService implements RespondToIncidentUseCase {

    private final IncidentKnowledgePort knowledgePort;
    private final IncidentHistoryPort historyPort;
    private final AlertPolicyPort alertPolicyPort;
    private final IncidentAnalysisService analysisService;
    private final IncidentActionRecommendationService actionRecommendationService;

    public IncidentResponderService(
            IncidentKnowledgePort knowledgePort,
            IncidentHistoryPort historyPort,
            AlertPolicyPort alertPolicyPort,
            IncidentAnalysisService analysisService,
            IncidentActionRecommendationService actionRecommendationService
    ) {
        this.knowledgePort = knowledgePort;
        this.historyPort = historyPort;
        this.alertPolicyPort = alertPolicyPort;
        this.analysisService = analysisService;
        this.actionRecommendationService = actionRecommendationService;
    }

    @Override
    public IncidentResponse respond(IncidentRequest request) {
        if (request.symptoms() == null || request.symptoms().isEmpty()) {
            throw new IncidentException("symptoms must not be empty");
        }

        Incident incident = new Incident(
                UUID.randomUUID(),
                request.sourceSystem(),
                request.title(),
                request.summary(),
                request.severity(),
                IncidentStatus.OPEN,
                request.symptoms(),
                OffsetDateTime.now()
        );

        var runbookSnippets = knowledgePort.findRelevantRunbookSnippets(
                incident.sourceSystem(),
                incident.summary(),
                incident.symptoms()
        );

        var similarIncidents = historyPort.findSimilarIncidents(
                incident.sourceSystem(),
                incident.summary()
        );

        var escalationPolicy = alertPolicyPort.resolveEscalationPolicy(
                incident.sourceSystem(),
                incident.severity()
        );

        var analysis = analysisService.analyze(
                incident,
                runbookSnippets,
                similarIncidents,
                escalationPolicy
        );

        var actionBundle = actionRecommendationService.recommend(
                incident,
                analysis,
                escalationPolicy
        );

        return new IncidentResponse(
                incident.incidentId().toString(),
                incident.severity().name(),
                analysis,
                actionBundle.actions(),
                similarIncidents,
                escalationPolicy,
                actionBundle.responseNote()
        );
    }
}
