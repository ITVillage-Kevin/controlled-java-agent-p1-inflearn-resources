package com.itvillage.incident.adapter.out.ai;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.time.OffsetDateTime;
import java.util.Map;

public class IncidentTools {

    @Tool(description = "Get current UTC time")
    public String getCurrentUtcTime() {
        return OffsetDateTime.now().toString();
    }

    @Tool(description = "Get response guideline by incident severity")
    public Map<String, String> getSeverityGuideline(
            @ToolParam(description = "Severity level such as LOW, MEDIUM, HIGH, CRITICAL")
            String severity) {
        return switch (severity.toUpperCase()) {
            case "LOW" -> Map.of("guideline", "Monitor and document. Avoid unnecessary escalation.");
            case "MEDIUM" -> Map.of("guideline", "Prepare owner notification and validate downstream impact.");
            case "HIGH" -> Map.of("guideline", "Immediate technical triage is required.");
            case "CRITICAL" -> Map.of("guideline", "Immediate escalation and multi-team coordination required.");
            default -> Map.of("guideline", "Unknown severity. Request human review.");
        };
    }
}
