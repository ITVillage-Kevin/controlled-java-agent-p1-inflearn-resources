package com.itvillage.incident.adapter.out.knowledge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itvillage.common.config.SampleDataProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RunbookCatalog {

    private final ResourcePatternResolver resourcePatternResolver;
    private final ObjectMapper yamlObjectMapper;
    private final SampleDataProperties sampleDataProperties;

    private final Map<String, List<RunbookEntry>> entriesByService = new ConcurrentHashMap<>();

    public RunbookCatalog(ResourcePatternResolver resourcePatternResolver,
                          @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
                          SampleDataProperties sampleDataProperties) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.yamlObjectMapper = yamlObjectMapper;
        this.sampleDataProperties = sampleDataProperties;
    }

    @PostConstruct
    void load() {
        try {
            Resource[] resources = resourcePatternResolver.getResources(sampleDataProperties.runbooksLocation());

            for (Resource resource : resources) {
                try (InputStream inputStream = resource.getInputStream()) {
                    RunbookDocument document = yamlObjectMapper.readValue(
                            inputStream,
                            RunbookDocument.class
                    );

                    List<RunbookEntry> entries = document.entries() == null
                            ? List.of()
                            : List.copyOf(document.entries());

                    entriesByService.put(
                            normalize(document.service()),
                            entries
                    );
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to load local runbook sample data.",
                    exception
            );
        }
    }

    public List<RunbookEntry> findByService(String sourceSystem) {
        return entriesByService.getOrDefault(normalize(sourceSystem), List.of());
    }

    public List<RunbookEntry> findDefaultEntries() {
        return entriesByService.getOrDefault("common", List.of());
    }

    public List<RunbookEntry> sortByPriorityAndRecency(List<RunbookEntry> entries) {
        return entries.stream()
                .sorted(
                        Comparator.comparingInt(RunbookEntry::priority)
                                .reversed()
                                .thenComparing(
                                        RunbookEntry::updatedAt,
                                        Comparator.reverseOrder()
                                )
                )
                .toList();
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
