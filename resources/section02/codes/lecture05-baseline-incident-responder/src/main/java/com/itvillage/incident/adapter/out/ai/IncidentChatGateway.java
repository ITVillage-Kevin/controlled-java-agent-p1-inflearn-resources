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

    public IncidentAiAnalysisOutput analyze(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .tools(incidentTools)
                .call()
                .entity(IncidentAiAnalysisOutput.class);
    }

    public IncidentAiActionOutput generateActions(String prompt) {
        return chatClient.prompt()
                .user(prompt)
                .tools(incidentTools)
                .call()
                .entity(IncidentAiActionOutput.class);
    }
}
