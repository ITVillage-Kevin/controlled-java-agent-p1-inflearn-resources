package com.itvillage.incident.domain;

public record RunbookSnippet(String runbookId,
                             String title,
                             int priority,
                             String content) {
}
