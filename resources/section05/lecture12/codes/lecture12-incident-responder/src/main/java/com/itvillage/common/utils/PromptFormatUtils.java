package com.itvillage.common.utils;

public final class PromptFormatUtils {

    private PromptFormatUtils() {
    }

    public static String jsonOnlyInstruction() {
        // QUALITY POINT:
        // 이 메서드는 LLM에게 JSON만 반환하라고 "텍스트로 지시"할 뿐이다.
        //
        // LLM이 실제로 다음을 반환해도 baseline은 감지하지 못한다.
        //
        // ```json
        // { ... }
        // ```
        //
        // 또는:
        // Here is the result:
        // { ... }
        //
        // 즉, Prompt 지시는 있지만
        // 지시 위반 감지, JSON schema 검증, 재시도, fallback이 없다.

        // 개선 방향:
        // ChatClient 매핑 실패 감지,
        // DTO validator, retry, fallback으로 보완한다.
        return """
                Return JSON only.
                Do not include markdown fences.
                Do not add explanations before or after JSON.
                """;
    }
}
