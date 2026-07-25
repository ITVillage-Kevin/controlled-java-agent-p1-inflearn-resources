package com.itvillage.incident.adapter.out.knowledge;

import com.itvillage.incident.port.out.IncidentKnowledgePort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StaticRunbookKnowledgeAdapter implements IncidentKnowledgePort {

    @Override
    public List<String> findRelevantRunbookSnippets(String sourceSystem, String summary, List<String> symptoms) {
        List<String> snippets = new ArrayList<>();

        if ("order-service".equalsIgnoreCase(sourceSystem)) {
            snippets.add("Check database connection pool saturation and recent connection timeout spikes.");
            snippets.add("Review application logs for SQLTransientConnectionException or lock timeout patterns.");
            snippets.add("If read traffic is elevated, verify replica health and failover routing state.");
        }

        if (summary.toLowerCase().contains("timeout")) {
            snippets.add("Timeout incidents often require distinguishing DB bottleneck, thread pool exhaustion, and downstream latency.");
        }

        if (symptoms.stream().anyMatch(s -> s.toLowerCase().contains("500"))) {
            snippets.add("A sudden rise in HTTP 500 errors may indicate connection pool starvation or exception fan-out at the controller/service boundary.");
        }

        if (snippets.isEmpty()) {
            snippets.add("Start with service health, dependency health, recent deployments, and top error logs.");
        }

        return snippets;
    }
}
