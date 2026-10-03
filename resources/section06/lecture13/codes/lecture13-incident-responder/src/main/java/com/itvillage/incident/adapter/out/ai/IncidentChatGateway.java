package com.itvillage.incident.adapter.out.ai;

import com.itvillage.incident.application.quality.IncidentQualityEvaluation;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class IncidentChatGateway {

    private final ChatClient chatClient;

    private final JsonOnlyResponseValidator jsonOnlyResponseValidator;

    private final IncidentAiOutputContract outputContract;

    public IncidentChatGateway(ChatClient.Builder builder,
                               JsonOnlyResponseValidator jsonOnlyResponseValidator,
                               IncidentAiOutputContract outputContract) {
        this.chatClient = builder.build();

        // 수업 11: 추가
        this.jsonOnlyResponseValidator = jsonOnlyResponseValidator;

        // 수업 11: 추가
        this.outputContract = outputContract;
    }

    public IncidentAiAnalysisOutput analyze(String prompt) {
        // 기존 .entity(...) 직접 변환 대신 먼저 raw 모델 응답을 받는다.
        //
        // 중요:
        // provider 호출은 try/catch 밖에 둔다.
        // timeout / 인증 / rate limit 같은 provider failure를
        // mapping failure로 잘못 분류하지 않기 위해서다.
        String rawContent = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

        // JSON 앞뒤 설명이나 markdown fence를 record 매핑 전에 검사한다.
        jsonOnlyResponseValidator.validate(rawContent);

        try {
            // JSON-only 검사를 통과한 raw response를 명시적으로 Analysis output으로 변환한다.
            return outputContract.convertAnalysis(rawContent);
        } catch (RuntimeException exception) {
            // JSON → Java output 변환 실패만 품질 흐름에서 구분할 수 있는 예외로 감싼다.
            throw new AiOutputMappingException("Failed to map AI analysis output.", exception);
        }
    }

    public IncidentAiActionOutput generateActions(String prompt) {
        String rawContent = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertActions(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map AI action output.", exception);
        }
    }

    // 수업 12: 추가
    public IncidentQualityEvaluation evaluateQuality(String prompt) {
        String rawContent = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertQualityEvaluation(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map AI quality evaluation output.", exception);
        }
    }

    // 수업 12: 추가
    public IncidentAiAnalysisOutput optimizeAnalysis(String prompt) {
        String rawContent = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertAnalysis(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map optimized AI analysis output.", exception);
        }
    }

    // 수업 12: 추가
    public IncidentAiActionOutput optimizeActions(String prompt) {
        String rawContent = chatClient.prompt()
                        .user(prompt)
                        .call()
                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertActions(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map optimized AI action output.", exception);
        }
    }
    // QUALITY POINT:
    // ChatClient가 LLM 응답을 IncidentAiAnalysisOutput으로 매핑한다.
    //
    // 하지만 LLM이 JSON 앞뒤에 설명을 붙이거나,
    // Markdown code fence를 포함하거나,
    // 필드 타입을 잘못 반환하거나,
    // 필수 필드를 누락하면 record 생성 이전의 매핑 단계에서 실패할 수 있다.
    //
    // 현재 baseline에는 이 매핑 실패를 구분하고,
    // 위반 사유를 Prompt에 포함해 재시도하거나,
    // 안전한 fallback으로 전환하는 흐름이 없다.
    //
    // 개선 방향:
    // mapping failure → retry → fallback 흐름을 추가한다.
//    public IncidentAiAnalysisOutput analyze(String prompt) {
//        return chatClient.prompt()
//                .user(prompt)
//                .call()
//                .entity(IncidentAiAnalysisOutput.class);
//    }

    // QUALITY POINT:
    // recommendedActions가 배열이 아닌 문자열로 오거나,
    // ActionItem 필드 타입이 맞지 않거나,
    // JSON 외 텍스트가 포함되면 IncidentAiActionOutput 생성 이전의
    // JSON 매핑 단계에서 실패할 수 있다.
    //
    // 현재 baseline에는 mapping failure 원인을 구분하고,
    // 재시도 또는 fallback ActionBundle로 전환하는 흐름이 없다.
    //
    // 개선 방향:
    // mapping failure → retry → fallback을 구현한다.
//    public IncidentAiActionOutput generateActions(String prompt) {
//        return chatClient.prompt()
//                .user(prompt)
//                .call()
//                .entity(IncidentAiActionOutput.class);
//    }
}
