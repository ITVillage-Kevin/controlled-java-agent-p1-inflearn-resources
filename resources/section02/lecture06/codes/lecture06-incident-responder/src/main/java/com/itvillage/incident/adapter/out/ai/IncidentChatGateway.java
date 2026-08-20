package com.itvillage.incident.adapter.out.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class IncidentChatGateway {

    private final ChatClient chatClient;
    private final IncidentTools incidentTools;

    public IncidentChatGateway(ChatClient.Builder builder, IncidentTools incidentTools) {
        this.chatClient = builder.build();
        this.incidentTools = incidentTools;
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
    public IncidentAiAnalysisOutput analyze(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .tools(incidentTools)
                .call()
                .entity(IncidentAiAnalysisOutput.class);
    }

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
    public IncidentAiActionOutput generateActions(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .tools(incidentTools)
                .call()
                .entity(IncidentAiActionOutput.class);
    }
}
