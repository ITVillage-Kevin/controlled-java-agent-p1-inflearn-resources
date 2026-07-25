package com.itvillage.incident.dto;

import com.itvillage.incident.domain.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record IncidentRequest(@NotBlank String sourceSystem,
                              @NotBlank String title,
                              @NotBlank String summary,
                              @NotNull IncidentSeverity severity,
                              @NotEmpty List<String> symptoms) {
}
