# lecture06-baseline-incident-responder-marked

수업 6에서 사용하는 코드는 수업 5의 baseline Incident Responder 앱과 **완전히 같은 코드**입니다.
클래스도, 메서드도, 로직도 바뀐 것이 없습니다..

달라진 것은 단 하나, 이 코드 위에 `// COST POINT`, `// QUALITY POINT`, `// RISK POINT`, `// FLOW POINT`라는
네 종류의 주석이 통제 4축(비용·품질·리스크·흐름) 관점에서 미리 표시되어 있다는 점입니다.

즉, 이 스냅샷의 목적은 새로운 기능을 보는 것이 아니라, **이미 알고 있는 코드 위에서 "숨어 있던 문제"의 정확한 위치를 확인하는 것**입니다.

---

## 이 코드 스냅샷에서 달라진 것

- 앱의 패키지 구조, 클래스, 메서드, 흐름: 수업 5와 100% 동일
- 추가된 것: 10개 클래스에 삽입된 통제 4축 마킹 주석뿐
- 마킹 주석은 코드를 실행하는 데 아무 영향을 주지 않는 순수 주석(comment)입니다. 실행 결과, 응답 형식, 엔드포인트는 수업 5와 동일합니다.
- 이번 스냅샷에서는 이 마킹 주석을 **고치지 않습니다**. 문제를 실제로 해소하는 코드 변경은 이후 수업에서 순서대로 진행됩니다.

---

## 수업 6에서의 학습 포인트

수업 6에서는 이 프로젝트를 가지고 아래 내용을 확인합니다.

- 비용·품질·리스크·흐름이라는 통제 4축이 실제 코드에서는 어떤 모습으로 나타나는가
- 어떤 클래스, 어떤 메서드에 어떤 축의 문제가 마킹되어 있는가
- 왜 그 지점이 그 축의 문제로 분류되었는가
- 마킹된 지점들이 서로 어떻게 연결되어 있는가 (예: 데이터를 만드는 지점과 소비하는 지점이 나란히 비용 축에 걸리는 경우)

수업 6도 수업 5와 마찬가지로 **구현 수업이 아니라 코드 읽기 수업**입니다. 새 코드를 작성하거나 문제를 고치지 않고, 이미 표시되어 있는 마킹 지점을 확인하는 데 집중합니다.

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section02/
    lecture06/
      codes/
        README.md
        lecture06-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture06-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

---

## 실행 환경

- Java 21
- Spring Boot 3.x
- Spring AI 1.1.4
- Gradle

## 실행 전 준비

OpenAI API Key가 필요합니다.

환경 변수 예시:

```bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

> 사용 중인 `application.yml` 설정에 따라 키 이름이 다를 수 있으니, 실제 프로젝트 설정 파일을 함께 확인하세요.

---

## 실행 방법

### 1) 터미널에서 실행

프로젝트 루트에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

---

## 기본 포트

기본 실행 포트는 다음과 같습니다.

```text
18080
```

따라서 엔드포인트 호출 주소는 아래와 같습니다.

```text
POST http://localhost:18080/api/incidents/respond
```

---

## API 개요

### Endpoint

```http
POST /api/incidents/respond
```

수업 5와 동일한 요청/응답 형식을 그대로 사용합니다. 요청 예시와 응답 예시는 수업 5 README를 참고하세요.

---

## 통제 4축 마킹 포인트 한눈에 보기

아래 클래스에 통제 4축 관점의 마킹 주석이 삽입되어 있습니다.

| 축 | 클래스 / 메서드 | 파일 위치 |
|---|---|---|
| 비용 | `IncidentPromptFactory` | `incident.adapter.out.ai` |
| 비용 | `LocalRunbookKnowledgeAdapter` | `incident.adapter.out.knowledge` |
| 비용 | `LocalIncidentHistoryAdapter` | `incident.adapter.out.history` |
| 품질 | `IncidentAiAnalysisOutput` | `incident.adapter.out.ai` |
| 품질 | `IncidentAiActionOutput` | `incident.adapter.out.ai` |
| 품질 | `PromptFormatUtils.jsonOnlyInstruction()` | `common.utils` |
| 품질 | `IncidentChatGateway` | `incident.adapter.out.ai` |
| 품질 | `IncidentAnalysisService` | `incident.application` |
| 품질 | `IncidentActionRecommendationService` | `incident.application` |
| 리스크 | `IncidentActionRecommendationService` | `incident.application` |
| 리스크 | `LocalAlertPolicyAdapter` | `incident.adapter.out.policy` |
| 흐름 | `IncidentResponderService` | `incident.application` |
| 흐름 | `TraceLoggingFilter` | `observability` |
| 흐름 | `AiObservationSupport` | `observability` |

수업 6에서는 위 지점이 실습의 전부입니다. 나머지 클래스는 수업 5에서 이미 다룬 그대로이며, 이번 수업에서 다시 보지 않습니다.

---

## 통제 4축별 마킹 포인트 자세히 보기


### 흐름(Flow) — 분기 없는 직선 흐름과 연결되지 않은 관찰 지점

흐름 축은 다음 두 가지를 봅니다.

```text
1. 요청이 실제로 어떤 단계와 시간으로 처리되었는가?
2. 요청 특성에 따라 적절한 처리 경로를 선택하는가?
```

#### 1. 모든 요청이 같은 선형 경로를 따름

- `IncidentResponderService.respond()`
  - LOW severity의 notification-service 지연과 CRITICAL payment-service 장애가 모두 같은 순서로 처리됩니다.

```text
knowledge 조회
  → history 조회
  → policy 조회
  → analysis LLM 호출
  → action recommendation LLM 호출
  → 응답 반환
```

  - severity, sourceSystem, symptoms에 따라 다른 handler를 선택하는 routing 또는 branching 구조가 없습니다.

```java
// FLOW POINT:
// 이 요청은 sourceSystem, severity, symptoms가 달라도
// 모두 동일한 선형 처리 경로를 따른다.
```

흐름 축 수업에서는 다음 route를 선택하도록 개선합니다.

```text
QUICK_TRIAGE
  → LOW severity 등 빠른 초기 분류가 필요한 요청

STANDARD_ANALYSIS
  → 런북·이력·정책 기반의 일반 분석이 필요한 요청

CRITICAL_ESCALATION
  → CRITICAL 또는 결제 위험 신호처럼
    긴급 에스컬레이션 중심 대응이 필요한 요청
```

> Routing은 “어느 경로로 보낼지”를 결정합니다.  
> Prompt와 컨텍스트 크기를 직접 제한하는 비용 통제는 수업 10에서 별도로 다룹니다.

#### 2. 서비스 단계가 trace·span·event로 연결되지 않음

- `IncidentResponderService`
  - knowledge, history, policy, analysis, action recommendation 단계가 실제 요청 파이프라인에서 어느 순서와 시간으로 실행됐는지 기록하지 않습니다.

- `TraceLoggingFilter`
  - UUID 기반 `traceId`를 MDC에 저장합니다.
  - MDC는 로그에 traceId 같은 공통 정보를 붙이는 임시 key-value 문맥입니다.
  - 따라서 같은 요청에서 발생한 로그를 traceId로 묶어 검색할 수 있습니다.
  - 하지만 현재 traceId는 MDC 로그에만 있으며, HTTP 응답 헤더, Micrometer Tracing span, OpenTelemetry trace, Langfuse trace, ChatClient 호출과 연결되지 않습니다.

- `AiObservationSupport`
  - `mark(stage, incidentId)` 메서드는 존재합니다.
  - 하지만 baseline에서는 `IncidentResponderService`, `IncidentAnalysisService`, `IncidentActionRecommendationService` 어디에서도 호출되지 않습니다.
  - 즉 관찰 도구는 준비되어 있지만 실제 요청 흐름에는 연결되지 않은 상태입니다.

```java
// FLOW POINT:
// knowledge, history, policy, analysis, action recommendation 단계가
// 실제 요청 파이프라인에서 어느 순서와 시간으로 실행됐는지
// 관찰 가능한 span / event로 남기지 않는다.
```

```java
// FLOW POINT:
// traceId는 현재 HTTP 요청 로그 MDC에만 저장된다.
//
// 따라서 로그에는 traceId가 보이더라도,
// 해당 요청이 AI 호출·Prompt·RAG를 어떻게 거쳤는지는 알 수 없다.
```

```java
// FLOW POINT:
// 관찰을 위한 메서드 자체는 존재한다.
//
// 하지만 baseline에서는 IncidentResponderService,
// IncidentAnalysisService,
// IncidentActionRecommendationService 어느 곳에서도
// 이 mark(...) 메서드를 실제로 호출하지 않는다.
```

흐름 축 수업에서는 다음을 연결합니다.

```text
HTTP 요청
  → Micrometer trace 생성
  → traceId를 MDC와 응답 헤더에 연결
  → IncidentResponderService span
  → knowledge-loaded event
  → history-loaded event
  → policy-resolved event
  → IncidentAnalysisService span
  → analysis-prompt-built event
  → ChatClient / Spring AI observation
  → IncidentActionRecommendationService span
  → action-prompt-built event
  → 최종 응답
```

> `fallback`은 Routing의 직접 해결 대상이 아닙니다.  
> JSON 매핑 실패와 출력 계약 검증 실패 후 retry/fallback은 수업 11의 품질 통제에서 다룹니다.

---

### 비용(Cost) — 압축 없이 커지는 컨텍스트

비용 축은 “LLM에 어떤 정보를 얼마나 많이 전달하는가”를 봅니다.

현재 baseline은 관련 정보를 찾고 정렬하는 기능은 있지만, Prompt에 넣을 정보의 개수와 길이를 제한하지 않습니다.

- `IncidentPromptFactory.buildAnalysisPrompt(...)`
  - incident 정보, 런북 목록, 유사 장애 이력, escalation policy를 Prompt에 그대로 넣습니다.
  - `runbookSnippets`, `similarIncidents`, `escalationPolicy`의 개수·길이·중복 여부를 제한하지 않습니다.

- `IncidentPromptFactory.buildActionPrompt(...)`
  - 분석 단계에서 생성된 `IncidentAiAnalysisOutput`의 모든 필드를 액션 추천 Prompt에 다시 넣습니다.
  - 분석 결과가 길어질수록 두 번째 LLM 호출의 입력 크기도 함께 커집니다.

- `LocalRunbookKnowledgeAdapter.findRelevantRunbookSnippets(...)`
  - 서비스와 `summary`, `symptoms`를 기준으로 관련 런북을 찾습니다.
  - `RunbookCatalog.sortByPriorityAndRecency(...)`를 통해 priority와 updatedAt 기준으로 정렬합니다.
  - 하지만 관련 런북이 여러 개이면 정렬된 결과를 모두 반환합니다.
  - 반환 항목 수, 총 문자 수, 유사 런북 중복, severity별 context budget을 제한하지 않습니다.

- `LocalIncidentHistoryAdapter.findSimilarIncidents(...)`
  - 같은 서비스의 과거 장애를 찾고, 유사도 점수와 최신성 기준으로 정렬합니다.
  - 하지만 유사도 점수가 있는 과거 장애 이력을 모두 반환합니다.
  - 반환 건수와 각 `resolutionSummary`의 길이를 제한하지 않습니다.

```java
// COST POINT:
// runbookSnippets와 similarIncidents의 개수·글자 수·중복 여부를 확인하지 않는다.
// adapter가 반환한 컨텍스트 전체가 그대로 Prompt에 들어간다.
```

```java
// COST POINT:
// 이 adapter는 서비스별 런북을 읽고,
// summary·symptoms로 관련 런북을 찾는다.
//
// 관련 런북이 없으면 기본 런북을 사용하고,
// 아래 sortByPriorityAndRecency(...) 호출에서
// priority·updatedAt 기준으로 정렬한다.
//
// 그러나 관련 런북이 여러 개면 정렬된 결과를 모두 반환한다.
```

```java
// COST POINT:
// baseline은 유사도 점수가 있는 모든 이력을 반환한다.
// 이력 개수와 resolutionSummary 길이의 제한이 없다.
```

비용 축 문제 개선 수업에서는 다음을 적용합니다.

```text
- 런북과 유사 장애 이력의 상위 N개 제한
- 각 런북 본문과 resolutionSummary의 최대 길이 제한
- 유사 런북 및 유사 추천 항목의 중복 제거
- severity별 context budget
- Prompt 전체 길이 및 토큰 예산 제한
- 분석 결과 전체 대신 액션 생성에 필요한 요약 정보만 재전달
```

---

### 품질(Quality) — 타입은 보장되지만 출력 계약과 의미는 보장되지 않음

품질 축은 “LLM 응답이 Java 객체로 바뀌었는가”만 보지 않습니다.

```text
1. JSON 텍스트를 DTO로 정상 매핑할 수 있는가?
2. DTO가 만들어진 뒤에도 필수 필드와 기본 계약을 만족하는가?
3. 분석과 추천 내용이 실제 장애 근거와 맞는가?
```

를 순서대로 봅니다.

#### 1. 타입 구조는 있지만 내용 검증이 없음

- `IncidentAiAnalysisOutput`
  - `situationSummary`, `likelyCauses`, `impactAssessment`, `confidenceNote` 필드를 가진 분석 결과 DTO입니다.
  - record이므로 필드의 타입 구조는 표현합니다.
  - 하지만 빈 문자열, 빈 원인 목록, 중복 원인, 근거 없는 confidence 표현을 막지 못합니다.

- `IncidentAiActionOutput`
  - `recommendedActions`, `responseNote`를 가진 액션 추천 결과 DTO입니다.
  - 액션 개수, 중복 actionTitle, 중복 priority, 빈 responseNote를 검증하지 않습니다.

- `IncidentAiActionOutput.ActionItem`
  - `priority`, `actionTitle`, `rationale`, `ownerSuggestion`을 가진 액션 한 건입니다.
  - `priority`가 허용 범위인지, 필수 필드가 비어 있지 않은지, rationale이 충분한 근거를 담는지는 검증하지 않습니다.

```java
// QUALITY POINT:
// 이 record는 JSON이 정상적으로 매핑된 경우의 타입 구조만 표현한다.
// 각 필드가 운영에 사용할 만큼 유효하고 신뢰할 수 있는지는 보장하지 않는다.
```

#### 2. JSON only 지시는 강제가 아님

- `PromptFormatUtils.jsonOnlyInstruction()`
  - LLM에게 JSON만 반환하라고 텍스트로 지시합니다.
  - 하지만 LLM이 Markdown code fence를 붙이거나, JSON 앞뒤에 설명 문장을 추가하거나, 잘못된 JSON을 반환해도 이를 감지하지 못합니다.
  - JSON schema 검증, 재시도, fallback이 없습니다.

```java
// QUALITY POINT:
// 이 메서드는 LLM에게 JSON만 반환하라고 "텍스트로 지시"할 뿐이다.
//
// 즉, Prompt 지시는 있지만
// 지시 위반 감지, JSON schema 검증, 재시도, fallback이 없다.
```

#### 3. ChatClient 매핑 실패를 구분하지 않음

- `IncidentChatGateway.analyze(...)`
  - LLM 응답을 `IncidentAiAnalysisOutput`으로 매핑합니다.
  - LLM이 JSON 외 설명 문장, Markdown code fence, 잘못된 JSON, 배열·문자열 타입 불일치, 필수 필드 누락을 반환하면 record 생성 이전의 매핑 단계에서 실패할 수 있습니다.

- `IncidentChatGateway.generateActions(...)`
  - LLM 응답을 `IncidentAiActionOutput`으로 매핑합니다.
  - `recommendedActions`가 배열이 아니거나, `priority`가 숫자가 아니거나, `ActionItem` 필드가 누락되면 매핑에 실패할 수 있습니다.

```java
// QUALITY POINT:
// ChatClient가 LLM 응답 텍스트를 IncidentAiAnalysisOutput으로 매핑한다.
//
// 하지만 LLM이 JSON 앞뒤에 설명을 붙이거나,
// Markdown code fence를 포함하거나,
// 필드 타입을 잘못 반환하거나,
// 필수 필드를 누락하면 record 생성 이전의 매핑 단계에서 실패할 수 있다.
//
// 현재 baseline에는 이 매핑 실패를 구분하고,
// 위반 사유를 Prompt에 포함해 재시도하거나,
// 안전한 fallback으로 전환하는 흐름이 없다.
```

#### 4. 매핑 성공 후 출력 계약을 검증하지 않음

- `IncidentAnalysisService.analyze(...)`
  - 분석 결과 DTO가 만들어져도 `situationSummary`, `impactAssessment`, `confidenceNote`가 비어 있는지 검증하지 않습니다.
  - `likelyCauses`가 비어 있거나 너무 많거나, 빈 원인을 포함하는지도 확인하지 않습니다.

- `IncidentActionRecommendationService.recommend(...)`
  - 액션 결과 DTO가 만들어져도 액션 개수, priority 범위, 중복 priority, 중복 actionTitle, 빈 rationale, 빈 ownerSuggestion을 검증하지 않습니다.

```java
// QUALITY POINT:
// ChatGateway 매핑에 성공해서 IncidentAiAnalysisOutput이 만들어졌더라도,
// 이 분석 결과가 운영에 사용할 최소 출력 계약을 만족하는지는 확인하지 않는다.
```

```java
// QUALITY POINT:
// ChatGateway가 IncidentAiActionOutput으로 매핑하는 데 성공했더라도,
// 추천 액션 목록이 최소 출력 계약을 만족하는지는 검증하지 않는다.
```

품질 축 수업에서는 다음을 구현합니다.

```text
LLM 호출
  → JSON → DTO 매핑 성공 여부 확인
  → DTO 기본 출력 계약 검증
  → mapping failure 또는 validation failure 발생
  → 위반 사유를 포함한 Prompt로 한 번 재시도
  → 재시도도 실패하면 안전한 fallback 결과 반환
```

```text
- 원인 후보가 incident, runbook, 과거 장애 이력과 실제로 연결되는가?
- impactAssessment가 운영 또는 비즈니스 영향을 구체적으로 설명하는가?
- confidenceNote가 근거보다 과도한 확신을 표현하지 않는가?
- 추천 액션의 priority와 rationale이 severity 및 장애 근거와 맞는가?
- actionTitle이 운영자가 실제로 수행할 만큼 구체적인가?
```

---

### 리스크(Risk) — 위험한 추천 노출과 구조화되지 않은 정책 결정

리스크 축은 “AI 추천이 위험한가”와 “정책이 실제 판단에 사용되는가”를 봅니다.

현재 Incident Responder는 복구 작업을 직접 실행하지 않습니다.

```text
Incident Responder
  → 분석 결과와 대응 추천을 반환
  → 위험도·승인 필요 여부·알림 필요 여부를 전달

호출자 또는 외부 시스템
  → 승인 요청 생성
  → Slack/PagerDuty 알림
  → 실제 후속 복구 절차 처리
```

#### 1. 위험한 추천을 그대로 전달

- `IncidentActionRecommendationService.recommend()`
  - LLM이 만든 `ActionItem`을 `RecommendedActionItem`으로 1:1 변환합니다.
  - restart, rollback, scale, replay 같은 위험한 문구가 있어도 위험도를 분류하지 않습니다.
  - 같은 추천이 반복돼도 중복 제거하지 않습니다.
  - `approvalRequired`, `riskLevel`, `riskReason` 같은 정보를 추가하지 않습니다.

```java
// RISK POINT:
// LLM이 생성한 ActionItem을 RecommendedActionItem으로 1:1 변환한다.
//
// 아직 없는 것:
// - restart, rollback, scale 같은 위험 키워드 필터
// - 동일·유사 액션 중복 제거
// - priority 범위 검증
// - 승인 필요 여부 표시
// - sourceSystem별 금지 또는 주의 액션 정책
```

#### 2. 서비스별 정책 데이터가 실제 판단에 반영되지 않음

- `LocalAlertPolicyAdapter.resolveEscalationPolicy()`
  - 정책 YAML에는 서비스별 businessCriticality, minimumSeverity와 severity별 requiresHumanApproval, notifyChannels가 존재합니다.
  - 하지만 baseline은 요청으로 들어온 severity를 그대로 사용합니다.
  - 예를 들어 `payment-service`의 minimumSeverity가 HIGH여도, MEDIUM 요청은 MEDIUM 정책을 그대로 선택합니다.
  - `requiresHumanApproval`, `notifyChannels`도 사람이 읽는 문자열에만 들어갑니다.

```java
// RISK POINT:
// 정책 YAML에는 다음 데이터가 이미 존재한다.
//
// - payment-service의 businessCriticality: CRITICAL
// - payment-service의 minimumSeverity: HIGH
// - HIGH/CRITICAL severity의 requiresHumanApproval: true
// - severity별 notifyChannels
//
// 하지만 baseline은 요청으로 들어온 severity를 그대로 사용한다.
//
// 예:
// payment-service + MEDIUM 요청
//   → minimumSeverity가 HIGH여도 MEDIUM severity 정책을 그대로 선택한다.
//
// 또한 requiresHumanApproval과 notifyChannels는
// 사람이 읽는 정책 문자열에만 포함된다.
```

리스크 축 수업에서는 다음을 구현합니다.

```text
추천 액션
  → 위험 키워드 분류
  → 중복 제거
  → riskLevel 추가
  → approvalRequired 추가
  → riskReason 추가

에스컬레이션 정책
  → requestedSeverity와 minimumSeverity 비교
  → effectiveSeverity 결정
  → CRITICAL이면 warRoomRequired 결정
  → approvalRequired, notifyChannels를 구조화
  → EscalationDecision 반환
```

호출자 또는 외부 시스템은 `EscalationDecision`의 `approvalRequired`, `notifyChannels`, `warRoomRequired`를 사용해 승인 요청, 알림, 후속 실행 절차를 처리합니다.

---

## 수업 6에서 코드를 볼 때의 권장 순서

수업 6에서는 아래 순서로 마킹 지점을 확인하는 것을 권장합니다.

1. `IncidentResponderService.respond()` — 전체 요청 흐름과 선형 처리 구조 확인
2. `IncidentPromptFactory` — Prompt에 컨텍스트가 쌓이는 비용 지점 확인
3. `LocalRunbookKnowledgeAdapter` — 런북 반환량 통제 부재 확인
4. `LocalIncidentHistoryAdapter` — 과거 장애 이력 반환량 통제 부재 확인
5. `IncidentAiAnalysisOutput` — 분석 결과 DTO의 기본 계약 부재 확인
6. `IncidentAiActionOutput` / `ActionItem` — 액션 결과 DTO의 기본 계약 부재 확인
7. `PromptFormatUtils.jsonOnlyInstruction()` — JSON only 지시의 한계 확인
8. `IncidentChatGateway` — LLM 응답 DTO 매핑 실패 지점 확인
9. `IncidentAnalysisService` — 분석 결과 검증·retry·fallback 부재 확인
10. `IncidentActionRecommendationService.recommend()` — 액션 출력 품질 문제와 위험 추천 노출 문제 확인
11. `LocalAlertPolicyAdapter.resolveEscalationPolicy()` — 서비스별 정책 판단 부재 확인
12. `TraceLoggingFilter` — MDC traceId의 전파 한계 확인
13. `AiObservationSupport` — 호출되지 않는 관찰 지점 확인

이 순서는 전체 흐름을 먼저 확인한 뒤, 비용 → 품질 → 리스크 → 흐름 관찰성 순서로 마킹 포인트를 읽는 방식입니다.

---

## 수업 6 실습에서 하지 않는 것

이번 수업에서는 아래 작업을 하지 않습니다.

- 마킹된 문제를 실제로 고치는 코드 수정
- 새 코드 작성 또는 구조 리팩터링
- 런북·이력의 top N, 길이 제한, 중복 제거, Prompt budget 적용
- DTO validation, mapping failure 처리, retry, fallback 추가
- 위험 키워드 필터, riskLevel, approvalRequired 추가
- `effectiveSeverity`, `EscalationDecision` 구현
- Routing 분기와 handler 추가
- Observability 연결, span 및 event 추가
- 실제 승인 요청 생성, Slack/PagerDuty 알림 발송, 복구 작업 실행

이 마킹 포인트들은 이후 수업에서 축별로 순서대로 개선됩니다.

---

## 마킹 포인트를 읽을 때 자주 던질 질문

코드를 읽을 때 아래 질문을 계속 떠올리면 좋습니다.

- 이 마킹은 비용·품질·리스크·흐름 중 어느 축의 문제인가?
- 이 지점을 그대로 두면 실제로 어떤 상황에서 문제가 커지는가?
- 이 마킹 지점과 연결된 다른 마킹 지점이 있는가?
- 지금 구조에는 이 문제를 막는 장치가 왜 없는가?
- 이 문제는 이후 어느 수업에서 어떤 방식으로 해소되는가?

예를 들어 비용 축에서는 다음 연결을 볼 수 있습니다.

```text
LocalRunbookKnowledgeAdapter
  → 관련 런북을 여러 개 반환

LocalIncidentHistoryAdapter
  → 유사 장애 이력을 여러 개 반환

IncidentPromptFactory
  → 반환된 런북·이력 전체를 Prompt에 삽입

결과
  → Prompt 길이, input token, 비용, latency 증가 가능성
```

---

## 빠른 호출 테스트

앱 실행 후 아래 curl로 빠르게 테스트할 수 있습니다.

마킹 주석은 실행 결과에 영향을 주지 않으므로, 응답 형식과 엔드포인트 동작은 수업 5 baseline과 동일합니다.

```bash
curl -X POST http://localhost:18080/api/incidents/respond \
  -H "Content-Type: application/json" \
  -d '{
    "sourceSystem": "order-service",
    "title": "Checkout timeouts",
    "summary": "Users cannot complete orders intermittently",
    "severity": "HIGH",
    "symptoms": [
      "checkout API timing out",
      "error rate spike"
    ]
  }'
```

---

## 마무리

이 스냅샷의 목적은 새로운 기능을 배우는 것이 아니라, **이미 아는 baseline 코드 위에서 통제 4축 문제의 정확한 위치를 확인하는 것**입니다.

수업 6에서는 비용·품질·리스크·흐름 마킹 포인트가 어떤 코드 위치에 있고, 서로 어떻게 연결되는지 이해하면 충분합니다.

이후 수업에서는 다음 순서로 문제를 실제 코드 변경으로 해소합니다.

```text
수업 7
  → traceId, span, event, Langfuse 관찰성 연결

수업 9
  → severity·서비스·증상 기반 Routing

수업 10
  → 런북·이력·Prompt 컨텍스트 비용 제한

수업 11
  → JSON 매핑 실패, 출력 계약 검증, retry, fallback

수업 12
  → Evaluator-Optimizer 기반 의미적 품질 평가·개선

수업 14
  → 위험 추천 필터링과 구조화된 에스컬레이션 정책 결정

수업 15
  → Advisor를 통한 정책 문맥·공통 관찰 규칙 적용

수업 16
  → Orchestrator–Workers 기반 분석·추천 판단 역할 분리
```
