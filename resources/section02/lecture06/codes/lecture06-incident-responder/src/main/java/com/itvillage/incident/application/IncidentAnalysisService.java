package com.itvillage.incident.application;

import com.itvillage.incident.adapter.out.ai.IncidentAiAnalysisOutput;
import com.itvillage.incident.adapter.out.ai.IncidentChatGateway;
import com.itvillage.incident.adapter.out.ai.IncidentPromptFactory;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.IncidentAnalysisResult;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncidentAnalysisService {

    private final IncidentPromptFactory promptFactory;
    private final IncidentChatGateway chatGateway;

    public IncidentAnalysisService(IncidentPromptFactory promptFactory, IncidentChatGateway chatGateway) {
        this.promptFactory = promptFactory;
        this.chatGateway = chatGateway;
    }

    public IncidentAnalysisResult analyze(Incident incident,
                                          List<String> runbookSnippets,
                                          List<SimilarIncidentSummary> similarIncidents,
                                          String escalationPolicy) {
        String prompt = promptFactory.buildAnalysisPrompt(
                incident,
                runbookSnippets,
                similarIncidents,
                escalationPolicy
        );

        IncidentAiAnalysisOutput output = chatGateway.analyze(prompt);

        // QUALITY POINT:
        // JSON 매핑에 성공했더라도 output의 필수 필드, 빈 값,
        // likelyCauses 목록 개수, 기본 비즈니스 제약을 검증하지 않는다.
        //
        // 또한 ChatClient / JSON 매핑 실패 또는 output 검증 실패 시
        // 위반 사유를 포함해 재시도하거나 fallback 결과로 전환하는 흐름이 없다.
        //
        // 개선 방향:
        // - IncidentAnalysisOutputValidator
        // - mapping failure / validation failure 구분
        // - retry prompt
        // - fallback IncidentAnalysisResult
        // 를 추가한다.
        return new IncidentAnalysisResult(
                output.situationSummary(),
                output.likelyCauses(),
                output.impactAssessment(),
                output.confidenceNote()
        );
    }
}
