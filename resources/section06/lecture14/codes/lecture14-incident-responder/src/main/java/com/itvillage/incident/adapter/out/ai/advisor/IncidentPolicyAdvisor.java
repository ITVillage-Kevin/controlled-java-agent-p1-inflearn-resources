package com.itvillage.incident.adapter.out.ai.advisor;

import com.itvillage.incident.dto.EscalationDecision;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

// 수업 14: 추가
@Component
public class IncidentPolicyAdvisor implements BaseAdvisor {

    public static final String ESCALATION_POLICY_CONTEXT_KEY = "incident.escalation-policy";

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain advisorChain) {

        Object value = request.context().get(ESCALATION_POLICY_CONTEXT_KEY);

        if (!(value instanceof EscalationDecision escalationPolicy)) {
            return request;
        }

        String policyContext = formatPolicyContext(escalationPolicy);

        // 기존 User Prompt를 유지하면서 공통 정책 문맥을 추가하기 위해
        // 기존 Prompt와 policyContext를 하나의 UserMessage로 합친다.
        String originalUserText = request.prompt()
                .getUserMessage()
                .getText();

        String augmentedUserText = originalUserText + policyContext;

        return request.mutate()
                .prompt(request.prompt().augmentUserMessage(augmentedUserText))
                .build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response,
                                    AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private String formatPolicyContext(EscalationDecision policy) {
        return """

                COMMON INCIDENT POLICY CONTEXT

                Business criticality:
                %s

                Acknowledge within:
                %d minutes

                Notify channels:
                %s

                Recovery action approval required:
                %s

                Escalation instruction:
                %s

                Treat this policy as incident-response context.
                Do not claim that approval has already been granted.
                Do not claim that an external system change
                has already been executed.
                """.formatted(
                policy.businessCriticality(),
                policy.acknowledgeWithinMinutes(),
                String.join(", ", policy.notifyChannels()),
                policy.approvalRequired(),
                policy.instruction()
        );
    }
}
