package com.itvillage.incident.port.out;

import java.util.List;

public interface IncidentKnowledgePort {
    List<String> findRelevantRunbookSnippets(String sourceSystem, String summary, List<String> symptoms);
}
