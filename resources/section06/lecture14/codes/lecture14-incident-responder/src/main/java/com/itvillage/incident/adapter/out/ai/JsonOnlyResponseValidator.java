package com.itvillage.incident.adapter.out.ai;

import org.springframework.stereotype.Component;

@Component
public class JsonOnlyResponseValidator {

    public void validate(String rawContent) {
        if (rawContent == null || rawContent.isBlank()) {
            throw new AiOutputMappingException("AI response must not be blank.");
        }

        String trimmed = rawContent.trim();

        if (trimmed.contains("```")) {
            throw new AiOutputMappingException("AI response must not contain markdown fences.");
        }

        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {

            throw new AiOutputMappingException("AI response must contain a JSON object only.");
        }
    }
}
