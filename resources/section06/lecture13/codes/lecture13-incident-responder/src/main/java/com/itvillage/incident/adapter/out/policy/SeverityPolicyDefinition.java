package com.itvillage.incident.adapter.out.policy;

import java.util.List;

public record SeverityPolicyDefinition(int acknowledgeWithinMinutes,
                                       List<String> notifyChannels,
                                       boolean requiresApprovalForRecoveryActions,
                                       String responseGuidance) {
}
