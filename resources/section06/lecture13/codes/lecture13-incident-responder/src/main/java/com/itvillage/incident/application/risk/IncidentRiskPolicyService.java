package com.itvillage.incident.application.risk;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.RecommendedActionItem;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// 수업 13: 추가 - 위험한 액션들을 추천 목록에 포함시키지 않도록 정책을 적용하는 역할을 한다.
@Service
public class IncidentRiskPolicyService {
    private final ActionRiskClassifier classifier;

    public IncidentRiskPolicyService(ActionRiskClassifier classifier) {
        this.classifier = classifier;
    }

    public RiskControlledActions apply(Incident incident,
                                       List<RecommendedActionItem> generatedActions,
                                       String originalNote) {
        if (generatedActions == null)
            throw new IllegalArgumentException("generatedActions is required");

        List<RecommendedActionItem> retained = new ArrayList<>();
        Set<String> retainedActionKeys = new HashSet<>();
        int excluded = 0;

        for (RecommendedActionItem action : generatedActions.stream()
                        .sorted(Comparator.comparingInt(RecommendedActionItem::priority)).toList()) {
            ActionRiskAssessment risk = classifier.classify(incident, action);

            if (!risk.allowed() || !retainedActionKeys.add(risk.deduplicationKey())) {
                excluded++;
                continue;
            }

            retained.add(
                    new RecommendedActionItem(
                            retained.size() + 1,
                            action.actionTitle(),
                            action.rationale(),
                            action.ownerSuggestion(),
                            risk.riskLevel().name(),
                            risk.approvalRequired(),
                            risk.riskReason()));
        }

        String note = originalNote == null ? "" : originalNote;

        if (retained.isEmpty()) {
            note = note + " Policy result: no recommendations can be returned. "
                    + "Review the incident manually and determine the next response.";
        } else if (excluded > 0) {
            note = note + " Policy result: " + excluded
                    + " recommendation(s) excluded as duplicate or disallowed.";
        }

        return new RiskControlledActions(List.copyOf(retained), note.trim(), excluded);
    }
}
