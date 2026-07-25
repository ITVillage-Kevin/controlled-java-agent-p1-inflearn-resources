package com.itvillage.common.utils;

public final class PromptFormatUtils {

    private PromptFormatUtils() {
    }

    public static String jsonOnlyInstruction() {
        return """
                Return JSON only.
                Do not include markdown fences.
                Do not add explanations before or after JSON.
                """;
    }
}
