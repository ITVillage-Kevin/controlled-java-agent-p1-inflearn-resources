package com.itvillage.incident.adapter.out.persistence;

import com.itvillage.incident.dto.SimilarIncidentSummary;
import com.itvillage.incident.port.out.IncidentHistoryPort;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InMemoryIncidentHistoryAdapter implements IncidentHistoryPort {

    @Override
    public List<SimilarIncidentSummary> findSimilarIncidents(String sourceSystem, String summary) {
        return List.of(
                new SimilarIncidentSummary(
                        "INC-2026-0412",
                        "Order API timeout due to exhausted DB connections",
                        "Reduced connection pool pressure, restarted stuck pods, and tuned datasource threshold."
                ),
                new SimilarIncidentSummary(
                        "INC-2026-0328",
                        "Checkout latency spike after traffic burst",
                        "Rate-limited hot endpoint traffic and validated replica lag before recovery."
                )
        );
    }
}
