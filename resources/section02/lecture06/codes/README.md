# lecture06-baseline-incident-responder-marked

수업 6에서 사용하는 코드는 수업 5의 baseline Incident Responder 앱과 **완전히 같은 코드**입니다.
클래스도, 메서드도, 로직도 바뀐 것이 없습니다.

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

아래 10개 클래스에 통제 4축 관점의 마킹 주석이 삽입되어 있습니다.

| 축 | 클래스 / 메서드 | 파일 위치 |
|---|---|---|
| 비용 | `IncidentPromptFactory` | `incident.adapter.out.ai` |
| 비용 | `StaticRunbookKnowledgeAdapter` | `incident.adapter.out.knowledge` |
| 품질 | `IncidentAiAnalysisOutput` | `incident.adapter.out.ai` |
| 품질 | `IncidentAiActionOutput` | `incident.adapter.out.ai` |
| 품질 | `PromptFormatUtils.jsonOnlyInstruction()` | `common.utils` |
| 리스크 | `IncidentActionRecommendationService.recommend()` | `incident.application` |
| 리스크 | `StaticAlertPolicyAdapter` | `incident.adapter.out.policy` |
| 흐름 | `IncidentResponderService.respond()` | `incident.application` |
| 흐름 | `TraceLoggingFilter` | `observability` |
| 흐름 | `AiObservationSupport` | `observability` |

수업 6에서는 위 10개 지점이 실습의 전부입니다. 나머지 클래스는 수업 5에서 이미 다룬 그대로이며, 이번 수업에서 다시 보지 않습니다.

---

## 통제 4축별 마킹 포인트 자세히 보기

### 비용(Cost) — 압축 없이 커지는 컨텍스트

- `IncidentPromptFactory.buildAnalysisPrompt` / `buildActionPrompt`: incident 전체, runbook snippet 전체, 유사 incident 전체, escalation policy 전체를 요약이나 길이 제한 없이 그대로 프롬프트에 이어붙입니다. `buildActionPrompt`는 분석 단계 결과까지 다시 통째로 실어 같은 데이터에 비용이 두 번 누적됩니다.
- `StaticRunbookKnowledgeAdapter.findRelevantRunbookSnippets`: 조건이 맞을 때마다 snippet을 `add()`만 하고, 개수나 총 글자 수를 제한하는 로직이 없습니다.

```java
// COST POINT: 이 메서드는 호출될 때마다 incident의 모든 필드,
// runbookSnippets 리스트 전체, similarIncidents 리스트 전체, escalationPolicy 문자열 전체를
// 요약·샘플링·길이 제한 없이 프롬프트 문자열에 그대로 이어붙인다.
```

### 품질(Quality) — 타입은 보장되지만 내용은 보장되지 않음

- `IncidentAiAnalysisOutput` / `IncidentAiActionOutput`(및 중첩 record `ActionItem`): record라서 필드 타입은 보장되지만, 값이 비어 있거나 무의미해도 이를 막는 검증 애노테이션이나 커스텀 validator가 없습니다.
- `PromptFormatUtils.jsonOnlyInstruction()`: "JSON만 반환하라"는 텍스트 지시일 뿐, LLM이 이를 어겨도 감지하거나 재시도하는 로직이 없습니다.

```java
// QUALITY POINT: record이므로 필드 타입은 컴파일 타임에 보장되지만,
// situationSummary가 빈 문자열이거나, likelyCauses가 빈 리스트이거나,
// confidenceNote가 의미 없는 값이어도 이 타입 자체는 이를 막을 방법이 없다.
```

### 리스크(Risk) — 필터링과 승인 없는 액션 전달

- `IncidentActionRecommendationService.recommend()`: LLM이 만든 액션을 필터링, 중복 제거, 위험도 검사, 승인 요구 표시 없이 그대로 `RecommendedActionItem`으로 매핑합니다.
- `StaticAlertPolicyAdapter.resolveEscalationPolicy()`: severity만 보고 정책 문자열을 반환하며, `sourceSystem`은 파라미터로 받지만 사용되지 않습니다. CRITICAL 정책조차 실행을 강제하는 장치 없이 문자열로만 존재합니다.

```java
// RISK POINT: LLM이 만든 액션(priority, actionTitle, rationale, ownerSuggestion)을
// 어떤 필터링, 중복 제거, 위험도 검사, 승인 요구 표시 없이 1:1로 그대로
// RecommendedActionItem으로 매핑한다.
```

### 흐름(Flow) — 분기 없는 직선 흐름과 연결되지 않은 관찰 지점

- `IncidentResponderService.respond()`: 입력 검증부터 응답 조립까지 하나의 직선 흐름으로만 진행되며, severity에 따른 분기나 fallback 경로가 없습니다.
- `TraceLoggingFilter`: traceId를 생성해 MDC에만 저장할 뿐, 응답 헤더나 이후 서비스 호출 로그로 전파되지 않습니다.
- `AiObservationSupport`: `mark(...)` 메서드가 준비되어 있지만, 어떤 서비스에도 주입되지 않아 baseline 전체에서 한 번도 호출되지 않는 "죽은 관찰 지점"입니다.

```java
// FLOW POINT: 여기서부터 analyze() → recommend() → 응답 조립까지
// 완전히 하나의 직선(linear) 흐름으로만 진행된다. severity가 LOW인지 CRITICAL인지에 따라
// 다른 경로를 타는 분기(branching)나, 특정 단계가 실패했을 때 대체 경로로 빠지는
// fallback path가 이 메서드 안에는 전혀 없다.
```

---

## 수업 6에서 코드를 볼 때의 권장 순서

수업 6에서는 아래 순서로 마킹 지점을 확인하는 것을 권장합니다.

1. `TraceLoggingFilter` (흐름)
2. `AiObservationSupport` (흐름)
3. `IncidentPromptFactory` (비용)
4. `StaticRunbookKnowledgeAdapter` (비용)
5. `IncidentAiAnalysisOutput` / `IncidentAiActionOutput` (품질)
6. `PromptFormatUtils.jsonOnlyInstruction()` (품질)
7. `IncidentActionRecommendationService.recommend()` (리스크)
8. `StaticAlertPolicyAdapter.resolveEscalationPolicy()` (리스크)
9. `IncidentResponderService.respond()` (흐름)

이 순서는 흐름 → 비용 → 품질 → 리스크 축 순서를 그대로 따르며, 슬라이드의 문제 지도 → 마킹 확인 → 코드 화면 전환 흐름과 자연스럽게 이어집니다.

---

## 수업 6 실습에서 하지 않는 것

이번 수업에서는 아래 작업을 하지 않습니다.

- 마킹된 문제를 실제로 고치는 코드 수정
- 새 코드 작성 또는 구조 리팩터링
- 검증 애노테이션, 필터링 로직, routing 분기 추가
- Observability 연결 (`AiObservationSupport` 주입 등)

이 마킹 포인트들은 이후 수업에서 축별로 순서대로 개선됩니다.

---

## 마킹 포인트를 읽을 때 자주 던질 질문

코드를 읽을 때 아래 질문을 계속 떠올리면 좋습니다.

- 이 마킹은 비용·품질·리스크·흐름 중 어느 축의 문제인가?
- 이 지점을 그대로 두면 실제로 어떤 상황에서 문제가 커지는가?
- 이 마킹 지점과 연결된 다른 마킹 지점이 있는가? (예: 데이터를 만드는 지점과 소비하는 지점)
- 지금 구조에는 이 문제를 막는 장치가 왜 없는가?

---

## 빠른 호출 테스트

앱 실행 후 아래 curl로 빠르게 테스트할 수 있습니다. 마킹 주석은 응답 결과에 영향을 주지 않으므로, 결과는 수업 5와 동일합니다.

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

이 스냅샷의 목적은 "새로운 코드를 배우는 것"이 아니라, **이미 아는 코드 위에서 통제 4축 문제의 정확한 위치를 확인하는 것**입니다.

수업 6에서는 위 10개 마킹 지점에 익숙해지는 것으로 충분합니다. 이 지점들을 실제로 통제하는 작업, 즉 비용 제한, 출력 검증, 위험 액션 필터링, 흐름 분기와 관찰성 연결은 다음 수업부터 순서대로 이어집니다.
