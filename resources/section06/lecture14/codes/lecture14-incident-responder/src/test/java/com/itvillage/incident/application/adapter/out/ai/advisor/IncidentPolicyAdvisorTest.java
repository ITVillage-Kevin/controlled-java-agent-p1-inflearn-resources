package com.itvillage.incident.application.adapter.out.ai.advisor;

import com.itvillage.incident.adapter.out.ai.advisor.IncidentPolicyAdvisor;
import com.itvillage.incident.domain.IncidentSeverity;
import com.itvillage.incident.dto.EscalationDecision;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class IncidentPolicyAdvisorTest {

    private final IncidentPolicyAdvisor advisor = new IncidentPolicyAdvisor();

    //  Advisor가 사용할 수 있는 ChatClientRequest의 Context에 escalationPolicy를 추가하는 경우,
    @Test
    void shouldAddEscalationPolicyContextToPrompt() {
        // given
        EscalationDecision escalationPolicy =
                new EscalationDecision(
                        "payment-service",
                        "CRITICAL",
                        IncidentSeverity.HIGH,
                        5,
                        List.of("slack-incident", "pager-duty"),
                        true,
                        "Escalate immediately to the incident commander."
                );

        ChatClientRequest request =
                new ChatClientRequest(
                        new Prompt("ORIGINAL INCIDENT PROMPT"),
                        Map.of(
                                IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY,
                                escalationPolicy
                        )
                );

        AdvisorChain advisorChain = mock(AdvisorChain.class);

        // when
        ChatClientRequest result =
                advisor.before(request, advisorChain);

        // then
        String userText =
                result.prompt()
                        .getUserMessage()
                        .getText();

        // 기존 Prompt가 사라지지 않아야 한다.
        assertThat(userText)
                .contains("ORIGINAL INCIDENT PROMPT");

        // Advisor가 공통 정책 컨텍스트를 추가해야 한다.
        assertThat(userText)
                .contains(
                        "COMMON INCIDENT POLICY CONTEXT",
                        "Business criticality:",
                        "CRITICAL",
                        "Acknowledge within:",
                        "5 minutes",
                        "Notify channels:",
                        "slack-incident, pager-duty",
                        "Recovery action approval required:",
                        "true",
                        "Escalation instruction:",
                        "Escalate immediately to the incident commander."
                );
    }

    // Context에 escalationPolicy를 추가하지 않을 경우
    @Test
    void shouldKeepOriginalRequestWhenEscalationPolicyIsMissing() {
        // given
        ChatClientRequest request =
                new ChatClientRequest(
                        new Prompt("ORIGINAL INCIDENT PROMPT"),
                        Map.of()
                );

        AdvisorChain advisorChain = mock(AdvisorChain.class);

        // when
        ChatClientRequest result =
                advisor.before(request, advisorChain);

        // then
        assertThat(result).isSameAs(request);
        assertThat(
                result.prompt()
                        .getUserMessage()
                        .getText()
        ).isEqualTo("ORIGINAL INCIDENT PROMPT");
    }
}
