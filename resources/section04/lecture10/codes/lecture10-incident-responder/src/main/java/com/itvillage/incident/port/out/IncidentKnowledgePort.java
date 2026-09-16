package com.itvillage.incident.port.out;

import com.itvillage.incident.domain.IncidentSeverity;

import java.util.List;

public interface IncidentKnowledgePort {
    List<String> findRelevantRunbookSnippets(String sourceSystem,
                                             String summary,
                                             List<String> symptoms,
                                             IncidentSeverity severity); // 수업 10: 추가
}
