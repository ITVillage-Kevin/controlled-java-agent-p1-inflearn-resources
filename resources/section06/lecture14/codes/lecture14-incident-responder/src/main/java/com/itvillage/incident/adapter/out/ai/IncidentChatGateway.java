package com.itvillage.incident.adapter.out.ai;

import com.itvillage.incident.adapter.out.ai.advisor.IncidentPolicyAdvisor;
import com.itvillage.incident.application.quality.IncidentQualityEvaluation;
import com.itvillage.incident.dto.EscalationDecision;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class IncidentChatGateway {

    private final ChatClient chatClient;
    private final JsonOnlyResponseValidator jsonOnlyResponseValidator;
    private final IncidentAiOutputContract outputContract;

    public IncidentChatGateway(ChatClient.Builder builder,
                               IncidentPolicyAdvisor policyAdvisor, // 수업 14: 추가
                               JsonOnlyResponseValidator jsonOnlyResponseValidator,
                               IncidentAiOutputContract outputContract) {
        this.chatClient = builder.defaultAdvisors(policyAdvisor).build(); // 수업 14: 변경

        this.jsonOnlyResponseValidator = jsonOnlyResponseValidator;
        this.outputContract = outputContract;
    }

    public IncidentAiAnalysisOutput analyze(String prompt,
                                            EscalationDecision escalationPolicy) { // 수업 14: 추가
        // 수업 14: 변경
        String rawContent = promptWithPolicy(prompt, escalationPolicy)
                        .call()
                        .content();
//        String rawContent = chatClient.prompt()
//                        .user(prompt)
//                        .call()
//                        .content();

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

    public IncidentAiActionOutput generateActions(String prompt,
                                                  EscalationDecision escalationPolicy) { // 수업 14: 추가
        // 수업 14: 변경
        String rawContent = promptWithPolicy(prompt, escalationPolicy)
                .call()
                .content();

//        String rawContent = chatClient.prompt()
//                        .user(prompt)
//                        .call()
//                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertActions(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map AI action output.", exception);
        }
    }

    public IncidentQualityEvaluation evaluateQuality(String prompt,
                                                     EscalationDecision escalationPolicy) { // 수업 14: 추가
        // 수업 14: 변경
        String rawContent = promptWithPolicy(prompt, escalationPolicy)
                .call()
                .content();

//        String rawContent = chatClient.prompt()
//                        .user(prompt)
//                        .call()
//                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertQualityEvaluation(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map AI quality evaluation output.", exception);
        }
    }

    public IncidentAiAnalysisOutput optimizeAnalysis(String prompt,
                                                     EscalationDecision escalationPolicy) { // 수업 14: 추가
        // 수업 14: 변경
        String rawContent = promptWithPolicy(prompt, escalationPolicy)
                .call()
                .content();

//        String rawContent = chatClient.prompt()
//                        .user(prompt)
//                        .call()
//                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertAnalysis(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map optimized AI analysis output.", exception);
        }
    }

    public IncidentAiActionOutput optimizeActions(String prompt,
                                                  EscalationDecision escalationPolicy) { // 수업 14: 추가
        // 수업 14: 변경
        String rawContent = promptWithPolicy(prompt, escalationPolicy)
                .call()
                .content();

//        String rawContent = chatClient.prompt()
//                        .user(prompt)
//                        .call()
//                        .content();

        jsonOnlyResponseValidator.validate(rawContent);

        try {
            return outputContract.convertActions(rawContent);
        } catch (RuntimeException exception) {
            throw new AiOutputMappingException("Failed to map optimized AI action output.", exception);
        }
    }

    // 수업 14: 추가
    private ChatClient.ChatClientRequestSpec promptWithPolicy(String prompt, EscalationDecision escalationPolicy) {

        return chatClient
                .prompt()
                .user(prompt)
                .advisors(advisorSpec -> advisorSpec
                        .param(
                                IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY,
                                escalationPolicy
                        )
                );
    }
}
