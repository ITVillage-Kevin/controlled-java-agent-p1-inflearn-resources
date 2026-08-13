package com.itvillage.incident.port.out;

import com.itvillage.incident.dto.SimilarIncidentSummary;

import java.util.List;

public interface IncidentHistoryPort {
    List<SimilarIncidentSummary> findSimilarIncidents(String sourceSystem, String summary);
}
