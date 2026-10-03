package com.itvillage.common.prompt;

// 수업 10: 추가
public final class PromptBudgetUtils {

    private static final String TRUNCATION_SUFFIX = "...";

    private PromptBudgetUtils() {}

    public static String truncate(String value, int maxCharacters) {
        if (maxCharacters < 0) {
            throw new IllegalArgumentException("maxCharacters must be zero or greater");
        }

        if (value == null || value.isBlank() || maxCharacters == 0) {
            return "";
        }

        String normalized = value.trim();

        if (normalized.length() <= maxCharacters) {
            return normalized;
        }

        if (maxCharacters <= TRUNCATION_SUFFIX.length()) {
            return normalized.substring(0, maxCharacters);
        }

        int contentLength = maxCharacters - TRUNCATION_SUFFIX.length();

        return normalized.substring(0, contentLength) + TRUNCATION_SUFFIX;
    }

    public static String requireWithinBudget(String value, int maxCharacters, String valueName) {
        if (value == null) {
            return "";
        }

        if (value.length() > maxCharacters) {
            throw new IllegalStateException(
                    "%s exceeded safety budget: %d > %d"
                            .formatted(valueName, value.length(), maxCharacters));
        }

        return value;
    }
}
