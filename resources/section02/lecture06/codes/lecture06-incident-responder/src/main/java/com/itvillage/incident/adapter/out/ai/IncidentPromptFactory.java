package com.itvillage.incident.adapter.out.ai;

import com.itvillage.common.utils.PromptFormatUtils;
import com.itvillage.incident.domain.Incident;
import com.itvillage.incident.dto.SimilarIncidentSummary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IncidentPromptFactory {

    public String buildAnalysisPrompt(Incident incident,
                                      List<String> runbookSnippets,
                                      List<SimilarIncidentSummary> similarIncidents,
                                      String escalationPolicy) {
        // COST POINT:
        // runbookSnippets와 similarIncidents의 개수·글자 수·중복 여부를 확인하지 않는다.
        // adapter가 반환한 컨텍스트 전체가 그대로 Prompt에 들어간다.
        //
        // 예:
        // - 여러 symptom이 동시에 매칭되면 런북 본문이 다수 추가된다.
        // - 유사 장애 이력이 많으면 resolutionSummary가 계속 누적된다.
        // - escalationPolicy도 길이 제한 없이 추가된다.
        //
        // 개선 방향:
        // - top N 제한
        // - 본문 길이 제한
        // - 중복 제거
        // - Prompt 토큰 예산 기반 캡핑
        // 을 적용한다.
        return """
                You are an incident analysis assistant for Korean operations teams.

                Analyze the incident and produce a concise operational analysis.

                Language rules:
                - Write every human-readable descriptive field value in Korean.
                - Use concise, natural, operational Korean suitable for real incident handling.
                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
                - If an English technical term is commonly used in practice, you may keep the term,
                  but the surrounding explanation must still be written in Korean.

                Writing rules:
                - Ground the analysis in the provided incident data, runbook snippets, similar incidents, and escalation policy.
                - Do not invent unsupported facts.
                - If uncertainty exists, state it clearly in Korean.
                - Keep the response short, practical, and operational.
                - likelyCauses must contain 2 to 4 short Korean items.

                Incident:
                - id: %s
                - sourceSystem: %s
                - title: %s
                - summary: %s
                - severity: %s
                - status: %s
                - symptoms: %s
                - occurredAt: %s

                Escalation policy:
                %s

                Runbook snippets:
                %s

                Similar incidents:
                %s

                Return fields:
                - situationSummary (Korean)
                - likelyCauses (array, 2~4 items, Korean)
                - impactAssessment (Korean)
                - confidenceNote (Korean)

                Important:
                - All descriptive response values must be written in Korean.
                - Do not translate identifiers or enum-like control values unnecessarily.
                - Return JSON only.

                %s
                """.formatted(
                incident.incidentId(),
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.status(),
                incident.symptoms(),
                incident.occurredAt(),
                escalationPolicy,
                runbookSnippets,
                similarIncidents,
                PromptFormatUtils.jsonOnlyInstruction()
        );
    }

    public String buildActionPrompt(Incident incident,
                                    IncidentAiAnalysisOutput analysisOutput,
                                    String escalationPolicy) {
        // COST POINT:
        // 분석 결과의 모든 필드를 다음 액션 Prompt에 그대로 재주입한다.
        // 분석 결과가 길어질수록 두 번째 LLM 호출의 입력 크기도 증가한다.
        //
        // 개선 방향:
        // 전체 분석 결과 대신 액션 생성에 필요한 요약 정보만 재전달하도록 개선한다.
        return """
                You are an incident response action planner for Korean operations teams.

                Based on the incident and analysis, propose practical immediate actions.

                Language rules:
                - Write every human-readable descriptive field value in Korean.
                - Use concise, actionable Korean suitable for on-call engineers and incident coordinators.
                - Do not write explanatory sentences in English unless the term is an unavoidable technical term.
                - Keep technical identifiers unchanged, including incident id, severity values, source system names,
                  service names, HTTP status codes, metric names, API paths, log fragments, class names, and proper nouns.
                - If an English technical term is commonly used in practice, you may keep the term,
                  but the surrounding explanation must still be written in Korean.

                Writing rules:
                - Recommend practical actions that can be executed immediately.
                - Prioritize actions by urgency and operational value.
                - Avoid vague recommendations.
                - Do not invent unsupported facts.
                - responseNote must be concise and written in Korean.

                Incident:
                - id: %s
                - sourceSystem: %s
                - title: %s
                - summary: %s
                - severity: %s
                - symptoms: %s

                Analysis:
                - situationSummary: %s
                - likelyCauses: %s
                - impactAssessment: %s
                - confidenceNote: %s

                Escalation policy:
                %s

                Return JSON with:
                - recommendedActions: array of 3 items
                  - priority
                  - actionTitle (Korean)
                  - rationale (Korean)
                  - ownerSuggestion (Korean where natural)
                - responseNote (Korean)

                Important:
                - All descriptive response values must be written in Korean.
                - Do not translate identifiers or enum-like control values unnecessarily.
                - Return JSON only.

                %s
                """.formatted(
                incident.incidentId(),
                incident.sourceSystem(),
                incident.title(),
                incident.summary(),
                incident.severity(),
                incident.symptoms(),
                analysisOutput.situationSummary(),
                analysisOutput.likelyCauses(),
                analysisOutput.impactAssessment(),
                analysisOutput.confidenceNote(),
                escalationPolicy,
                PromptFormatUtils.jsonOnlyInstruction()
        );
    }
}
