package com.itvillage.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sample-data")
public record SampleDataProperties(String runbooksLocation,
                                   String incidentHistoryLocation,
                                   String escalationPolicyLocation) {
}
