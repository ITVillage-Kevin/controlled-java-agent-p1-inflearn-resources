package com.itvillage.incident.application.quality;

import java.util.List;

public record ValidationResult(boolean valid, List<String> violations) {
    public static ValidationResult success() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(List<String> violations) {
        return new ValidationResult(false, List.copyOf(violations));
    }

    public static ValidationResult from(List<String> violations) {
        return violations.isEmpty() ? success() : failure(violations);
    }
}
