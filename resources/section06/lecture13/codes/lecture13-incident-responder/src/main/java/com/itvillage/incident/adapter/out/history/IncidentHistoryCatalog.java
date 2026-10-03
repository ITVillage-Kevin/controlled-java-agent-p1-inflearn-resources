package com.itvillage.incident.adapter.out.history;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itvillage.common.config.SampleDataProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;

@Component
public class IncidentHistoryCatalog {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;
    private final SampleDataProperties sampleDataProperties;

    private List<HistoricalIncidentRecord> incidents = List.of();

    public IncidentHistoryCatalog(ResourceLoader resourceLoader,
                                  @Qualifier("objectMapper") ObjectMapper objectMapper,
                                  SampleDataProperties sampleDataProperties) {
        this.resourceLoader = resourceLoader;
        this.objectMapper = objectMapper;
        this.sampleDataProperties = sampleDataProperties;
    }

    @PostConstruct
    void load() {
        try (InputStream inputStream = resourceLoader
                .getResource(sampleDataProperties.incidentHistoryLocation())
                .getInputStream()) {

            incidents = objectMapper.readValue(
                    inputStream,
                    new TypeReference<List<HistoricalIncidentRecord>>() {
                    }
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to load local incident history sample data.",
                    exception
            );
        }
    }

    public List<HistoricalIncidentRecord> findBySourceSystem(String sourceSystem) {
        String normalizedSourceSystem = normalize(sourceSystem);

        return incidents.stream()
                .filter(incident ->
                        normalize(incident.sourceSystem())
                                .equals(normalizedSourceSystem)
                )
                .toList();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
