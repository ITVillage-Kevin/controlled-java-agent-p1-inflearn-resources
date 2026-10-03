package com.itvillage.incident.application.risk;

import com.itvillage.incident.dto.RecommendedActionItem;

import java.util.List;

// 수업 13: 추가
public record RiskControlledActions(List<RecommendedActionItem> actions,
                                    String responseNote,
                                    int excludedCount) {
}
