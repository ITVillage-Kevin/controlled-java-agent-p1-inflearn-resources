package com.itvillage.incident.application.risk;

import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.RecommendedActionItem;
import org.springframework.stereotype.Component;

import java.util.Locale;

// 수업 13: 추가 - AI가 만든 추천 제목과 근거를 애플리케이션의 제한된 규칙으로 검사해 위험 평가 결과인 `ActionRiskAssessment`를 생성한다.
@Component
public class ActionRiskClassifier {
    public ActionRiskAssessment classify(Incident incident, RecommendedActionItem action) {
        String text = (action.actionTitle() + " " + action.rationale()).toLowerCase(Locale.ROOT);
        String title = action.actionTitle().toLowerCase(Locale.ROOT);

        // 강의용 결정론적 분류: 실제 운영에서는 문자열 검사만으로 추천의 안전성을 보장할 수는 없다
        boolean destructive = containsAny(text, "delete database", "drop table", "wipe data", "disable audit");
        boolean rollback = containsAny(text, "rollback", "roll back", "revert deployment");
        boolean restart = containsAny(text, "restart", "reboot", "redeploy", "scale down");
        boolean readOnly = containsAny(title, "check", "inspect", "review", "monitor", "verify", "collect");

        if (destructive) {
            return new ActionRiskAssessment(ActionRiskLevel.CRITICAL, true,
                    "Destructive or audit-disabling operation; excluded by application policy.",
                    "destructive-change", false);
        }
        if (rollback) {
            return new ActionRiskAssessment(ActionRiskLevel.HIGH, true,
                    "Deployment rollback changes the running service; external approval is required.",
                    "deployment-rollback", true);
        }
        if (restart) {
            return new ActionRiskAssessment(ActionRiskLevel.HIGH, true,
                    "Service restart or deployment change may interrupt traffic; external approval is required.",
                    "service-restart", true);
        }
        if (readOnly) {
            return new ActionRiskAssessment(ActionRiskLevel.LOW, false,
                    "Read-only investigation; no system change is requested.",
                    "read:" + title.replaceAll("[^a-z0-9]+", " ").trim(), true);
        }
        // 미분류 작업을 안전하다고 추정하지 않는다.
        return new ActionRiskAssessment(ActionRiskLevel.MEDIUM, true,
                "Action is not recognized as read-only; manual review is required.",
                "unknown:" + title.replaceAll("[^a-z0-9]+", " ").trim(), true);
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) return true;
        }
        return false;
    }
}
