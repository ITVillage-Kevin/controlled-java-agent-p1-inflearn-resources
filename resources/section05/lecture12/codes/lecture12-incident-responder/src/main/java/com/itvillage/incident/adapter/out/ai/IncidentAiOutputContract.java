package com.itvillage.incident.adapter.out.ai;

import com.itvillage.incident.application.quality.IncidentQualityEvaluation;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class IncidentAiOutputContract {

    private final BeanOutputConverter<IncidentAiAnalysisOutput> analysisConverter;

    private final BeanOutputConverter<IncidentAiActionOutput> actionConverter;

    // 수업 12: 추가
    private final BeanOutputConverter<IncidentQualityEvaluation> qualityEvaluationConverter;

    public IncidentAiOutputContract() {
        this.analysisConverter = new BeanOutputConverter<>(IncidentAiAnalysisOutput.class);

        this.actionConverter = new BeanOutputConverter<>(IncidentAiActionOutput.class);

        this.qualityEvaluationConverter = new BeanOutputConverter<>(IncidentQualityEvaluation.class);
    }

    public String analysisFormat() {
        return analysisConverter.getFormat();
    }

    public String actionFormat() {
        return actionConverter.getFormat();
    }

    // 수업 12: 추가
    public String qualityEvaluationFormat() {
        return qualityEvaluationConverter.getFormat();
    }

    public IncidentAiAnalysisOutput convertAnalysis(String rawContent) {
        return analysisConverter.convert(rawContent);
    }

    public IncidentAiActionOutput convertActions(String rawContent) {
        return actionConverter.convert(rawContent);
    }

    // 수업 12: 추가
    public IncidentQualityEvaluation convertQualityEvaluation(String rawContent) {
        return qualityEvaluationConverter.convert(rawContent);
    }
}
