package com.itvillage.incident.adapter.out.ai;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

// 수업 11: 추가
@Component
public class IncidentAiOutputContract {

    private final BeanOutputConverter<IncidentAiAnalysisOutput> analysisConverter;

    private final BeanOutputConverter<IncidentAiActionOutput> actionConverter;

    public IncidentAiOutputContract() {
        this.analysisConverter = new BeanOutputConverter<>(IncidentAiAnalysisOutput.class);

        this.actionConverter = new BeanOutputConverter<>(IncidentAiActionOutput.class);
    }

    public String analysisFormat() {
        return analysisConverter.getFormat();
    }

    public String actionFormat() {
        return actionConverter.getFormat();
    }

    public IncidentAiAnalysisOutput convertAnalysis(String rawContent) {
        return analysisConverter.convert(rawContent);
    }

    public IncidentAiActionOutput convertActions(String rawContent) {
        return actionConverter.convert(rawContent);
    }
}
