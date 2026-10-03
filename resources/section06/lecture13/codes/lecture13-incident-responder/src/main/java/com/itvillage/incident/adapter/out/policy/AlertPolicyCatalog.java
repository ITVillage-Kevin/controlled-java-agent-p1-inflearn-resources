package com.itvillage.incident.adapter.out.policy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itvillage.common.config.SampleDataProperties;
import com.itvillage.incident.domain.IncidentSeverity;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Locale;

@Component
public class AlertPolicyCatalog {

    private final ResourceLoader resourceLoader;
    private final ObjectMapper yamlObjectMapper;
    private final SampleDataProperties sampleDataProperties;

    private AlertPolicyDocument policyDocument;

    public AlertPolicyCatalog(ResourceLoader resourceLoader,
                              @Qualifier("yamlObjectMapper") ObjectMapper yamlObjectMapper,
                              SampleDataProperties sampleDataProperties) {
        this.resourceLoader = resourceLoader;
        this.yamlObjectMapper = yamlObjectMapper;
        this.sampleDataProperties = sampleDataProperties;
    }

    @PostConstruct
    void load() {
        try (InputStream inputStream = resourceLoader
                .getResource(sampleDataProperties.escalationPolicyLocation())
                .getInputStream()) {

            policyDocument = yamlObjectMapper.readValue(
                    inputStream,
                    AlertPolicyDocument.class
            );
        } catch (IOException exception) {
            throw new UncheckedIOException(
                    "Failed to load local escalation policy sample data.",
                    exception
            );
        }
    }

    public ServicePolicyDefinition findServicePolicy(
            String sourceSystem
    ) {
        return policyDocument.services().get(normalize(sourceSystem));
    }

    public SeverityPolicyDefinition findSeverityPolicy(
            IncidentSeverity severity
    ) {
        return policyDocument.severityPolicies().get(severity);
    }

    private String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(Locale.ROOT);
    }
}
