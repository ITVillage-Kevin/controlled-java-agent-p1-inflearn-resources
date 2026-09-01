# lecture07-spring-ai-observability

수업 7에서 사용하는 코드는 수업 6의 baseline Incident Responder에 **요청 흐름 관찰 구조를 실제로 연결한 코드**입니다.

수업 6에서는 `// FLOW POINT` 주석을 통해 “요청이 어느 단계를 거쳤는지 나중에 복기하기 어렵다”는 문제 위치를 읽었습니다. 수업 7에서는 그 문제 중 관찰성에 해당하는 부분을 개선합니다.

이 스냅샷의 목적은 로그를 많이 남기는 것이 아닙니다. **하나의 Incident 요청이 어떤 단계를 거쳤는지, 각 단계가 어느 service span 안에서 실행됐는지, 어디까지 진행됐는지, 실패했다면 어느 지점에서 실패했는지를 trace · span · event 구조로 연결하는 것**입니다.

> Langfuse, OTLP exporter, 외부 trace backend 전송, token usage 및 latency의 UI 분석은 수업 8에서 다룹니다.

---

## 이 코드 스냅샷에서 달라진 것

수업 6 코드와 비교해, 수업 7에서는 흐름 관찰을 위해 아래 항목을 추가하거나 변경했습니다.

| 항목 | 수업 6 baseline | 수업 7 변경 |
|---|---|---|
| `traceId` | Filter가 UUID를 직접 생성하고 MDC에만 저장 | Micrometer Tracing의 현재 `traceId`를 읽어 MDC와 `X-Trace-Id` 응답 헤더에 연결 |
| Service 관찰 | 서비스 실행 경계가 trace/span으로 남지 않음 | `respond()`, `analyze()`, `recommend()`에 `@Observed` 적용 |
| 업무 이정표 | `AiObservationSupport.mark(...)`가 실제 흐름에서 호출되지 않음 | 현재 Observation/span에 업무 event를 기록하도록 개선하고 서비스에서 호출 |
| 오류 관찰 | 요청 흐름의 현재 span에 오류를 남기지 않음 | service catch 블록에서 현재 Observation에 error 기록 |
| Spring AI 호출 컨텍스트 | 업무 service 흐름과 모델 호출의 관계를 읽기 어려움 | 분석/액션 추천 service span 안에서 ChatClient 호출이 이어질 수 있는 관찰 컨텍스트 구성 |

---

## 수업 7 학습 포인트

수업 7에서는 이 프로젝트를 통해 아래 내용을 확인합니다.

- 요청 한 건을 하나의 trace로 묶는다는 의미
- trace, span, event의 역할 차이
- `@Observed`가 업무적으로 의미 있는 service 메서드 경계를 관찰 대상으로 만드는 방식
- `AiObservationSupport.mark(...)`가 service span 중간에 업무 이정표를 기록하는 방식
- Micrometer의 현재 `traceId`를 MDC와 `X-Trace-Id` 응답 헤더에 연결하는 이유
- 일반 로그와 traceId를 연결해 콘솔에서 요청 하나를 따라가는 방법
- 분석과 액션 추천의 Spring AI `ChatClient` 호출이 각 service span의 하위 흐름으로 이어지는 구조
- 오류 발생 시 어느 단계까지 진행됐는지 traceId와 event 흐름으로 확인하는 방법

수업 7은 관찰성 연결을 실제로 적용하고, Postman 응답 header와 콘솔 로그로 동작을 확인하는 실습 수업입니다. 외부 UI에서 trace tree, generation, token usage, latency를 분석하는 작업은 수업 8에서 이어집니다.

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section03/
    lecture07/
      codes/
        README.md
        lecture07-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture07-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

---

## 실행 환경

- Java 21
- Spring Boot 3.5.13
- Spring AI 1.1.4
- Gradle
- OpenAI API Key

### 실행 전 준비

OpenAI API Key가 필요합니다.

```bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

> 사용 중인 `application.yml` 설정에 따라 환경 변수 이름이 다를 수 있으므로 실제 프로젝트 설정 파일을 함께 확인하세요.

수업 7에서는 Langfuse endpoint, Langfuse API key, OTLP Authorization 환경 변수를 설정하지 않습니다. 이 항목은 수업 8에서 추가합니다.

---

## 실행 방법

### 1) 터미널에서 실행

`lecture07-incident-responder/` 프로젝트 루트에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

---

## 기본 포트와 API

기본 실행 포트는 `18080`입니다.

```text
POST http://localhost:18080/api/incidents/respond
```

### Endpoint

```http
POST /api/incidents/respond
```

요청과 응답의 기본 구조는 수업 6 baseline과 동일합니다. 수업 7은 API 계약을 바꾸지 않고, 요청 처리 과정에 관찰 구조를 연결합니다.

### 빠른 호출 테스트

앱 실행 후 아래 요청으로 정상 흐름을 확인할 수 있습니다.

```bash
curl -i -X POST http://localhost:18080/api/incidents/respond \
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

`-i` 옵션을 사용하면 응답 header도 함께 확인할 수 있습니다. 정상적인 HTTP 요청 흐름에서는 아래와 같은 header를 확인할 수 있습니다.

```http
X-Trace-Id: <current-trace-id>
```

이 값은 호출자가 이번 요청을 처리한 trace를 조회할 수 있도록 응답에 넣는 편의용 header입니다.

> `X-Trace-Id`는 분산 trace를 이어받기 위한 표준 전파 header가 아닙니다. 서비스 간 trace 문맥 전파는 W3C `traceparent` header를 기준으로 합니다. 수업 7에서는 `traceparent`를 직접 파싱하지 않고, Micrometer가 보유한 현재 trace 문맥에서 `traceId`를 읽습니다.

---

## 요청 흐름

수업 7 코드가 애플리케이션 내부에서 만드는 관찰 구조는 다음과 같습니다.

```text
HTTP POST /api/incidents/respond
  └─ HTTP 요청 trace 문맥
      └─ respond-incident
          ├─ event: incident-created
          ├─ event: knowledge-loaded
          ├─ event: history-loaded
          ├─ event: policy-resolved
          ├─ analyze-incident
          │   ├─ event: analysis-prompt-built
          │   ├─ Spring AI ChatClient 분석 호출
          │   └─ event: analysis-ai-response-received
          ├─ event: analysis-completed
          ├─ recommend-actions
          │   ├─ event: action-prompt-built
          │   ├─ Spring AI ChatClient 액션 추천 호출
          │   └─ event: actions-ai-response-received
          └─ event: actions-recommended
```

위 구조는 수업 7에서 애플리케이션 내부의 Micrometer Observation 및 tracing 문맥 안에 만들어집니다. 아직 외부 trace backend로 export하지 않습니다.

수업 8에서 OTLP exporter와 Langfuse를 연결하면, 수업 7에서 만든 service span과 ChatClient 관찰 흐름을 외부 trace tree에서 확인할 수 있습니다.

---

## 핵심 코드 위치

수업 7에서 중점적으로 볼 파일은 아래와 같습니다.

| 파일 | 수업 7에서 확인할 책임 |
|---|---|
| `build.gradle` | Actuator, AOP, Micrometer Tracing OpenTelemetry bridge 의존성 |
| `src/main/resources/application.yml` | `@Observed` 활성화, tracing sampling, 로그 패턴 설정 |
| `observability/TraceLoggingFilter.java` | 현재 Micrometer `traceId`를 MDC와 `X-Trace-Id`에 연결 |
| `observability/AiObservationSupport.java` | 현재 Observation/span에 event와 error 기록 |
| `observability/IncidentObservationEvents.java` | 관찰 event 이름 상수 관리 |
| `incident/application/IncidentResponderService.java` | 요청 전체 조립 service span과 knowledge/history/policy 관련 event |
| `incident/application/IncidentAnalysisService.java` | 분석 service span, 분석 Prompt/AI 응답 event |
| `incident/application/IncidentActionRecommendationService.java` | 액션 추천 service span, 액션 Prompt/AI 응답 event |
| `incident/adapter/out/ai/IncidentChatGateway.java` | 분석 및 액션 추천을 위한 Spring AI `ChatClient` 호출 |

수업 7에서 수정하지 않고 유지하는 주요 파일은 아래와 같습니다.

```text
- IncidentPromptFactory
- LocalRunbookKnowledgeAdapter
- LocalIncidentHistoryAdapter
- LocalAlertPolicyAdapter
- IncidentRequest, IncidentResponse 등 DTO
- Incident 도메인 모델
- Port 인터페이스
- Controller
- 예외 처리 구조
```

---

## 관찰 구성 요소

### `@Observed`: service span 경계

`@Observed`는 “이 service 메서드 전체를 관찰하겠다”는 선언입니다. Spring AOP가 해당 메서드 호출을 감싸 Observation lifecycle을 처리합니다.

수업 7에서는 업무적으로 의미 있는 세 메서드에만 적용합니다.

```text
IncidentResponderService.respond()
  → 요청 전체 조립

IncidentAnalysisService.analyze()
  → 장애 원인 분석

IncidentActionRecommendationService.recommend()
  → 대응 액션 추천
```

`@Observed`는 모든 작은 메서드에 붙이지 않습니다. Prompt 문자열 조립, DTO 변환, 단순 Stream 처리처럼 작은 보조 작업까지 모두 span으로 만들면 trace가 너무 깊어져 오히려 읽기 어려워집니다.

### event: service span 안의 업무 이정표

event는 span 중간에 남기는 특정 시점의 이정표입니다.

```text
span
  → 시작과 종료 시각이 있어 duration을 계산할 수 있는 작업 구간

event
  → span 중간의 특정 시점
  → 자체 duration을 가지지 않는 업무 checkpoint
```

예를 들어 `knowledge-loaded`는 런북 조회가 끝난 시점이며, `analysis-prompt-built`는 분석 Prompt 조립이 끝난 시점입니다.

수업 7은 service 메서드 전체 경계는 `@Observed`로 만들고, service 내부에서만 알 수 있는 업무 이정표는 `AiObservationSupport.mark(...)`로 최소한 기록합니다.

### event 상수: `IncidentObservationEvents`

event 이름은 문자열 리터럴 대신 `IncidentObservationEvents`에서 관리합니다.

```text
INCIDENT_CREATED
KNOWLEDGE_LOADED
HISTORY_LOADED
POLICY_RESOLVED
ANALYSIS_PROMPT_BUILT
ANALYSIS_AI_RESPONSE_RECEIVED
ANALYSIS_COMPLETED
ACTION_PROMPT_BUILT
ACTIONS_AI_RESPONSE_RECEIVED
ACTIONS_RECOMMENDED
```

이 방식은 event 이름 오타를 줄이고, 서비스 간 이름을 일관되게 유지하며, 이후 trace backend에서 검색할 이름을 안정적으로 관리하기 위한 것입니다.

### `AiObservationSupport`: 기술 세부사항 분리

`AiObservationSupport`는 현재 `ObservationRegistry`에서 실행 중인 Observation을 찾아 다음을 수행합니다.

- `incident.id`를 high-cardinality key-value로 기록
- 현재 service span에 업무 event 기록
- 예외가 발생하면 현재 Observation에 error 기록

`incident.id`는 요청마다 값이 달라지는 high-cardinality 값입니다. 특정 요청 trace를 찾는 데는 유용하지만, severity처럼 값의 종류가 적은 metric tag와 같은 방식으로 다루면 시계열 수가 과도하게 늘어날 수 있습니다.

관찰 event를 기록하지 못하는 특수 실행 경로가 있어도 Incident 분석·추천 자체가 실패하면 안 되므로, 현재 Observation이 없으면 `mark(...)`는 예외 대신 안전하게 종료합니다.

### `TraceLoggingFilter`: trace와 일반 로그 연결

`TraceLoggingFilter`는 trace를 새로 만드는 컴포넌트가 아닙니다. Micrometer Tracing이 현재 요청에 만든 trace 문맥에서 `traceId`를 읽어 다음 두 곳에 연결합니다.

```text
현재 Micrometer traceId
  → MDC traceId
  → X-Trace-Id 응답 header
```

MDC는 일반 로그에 같은 요청의 traceId를 자동으로 붙이기 위한 임시 로그 문맥입니다. 요청 처리가 끝나면 `MDC.remove(...)`로 값을 제거합니다. 이는 이미 출력한 로그를 지우는 것이 아니라, 웹 서버가 재사용하는 스레드에 이전 요청의 traceId가 남지 않도록 정리하는 작업입니다.

---

## 정상 흐름 확인

정상 요청을 보낸 뒤에는 아래 항목을 확인합니다.

1. HTTP 응답이 정상적으로 반환되는지 확인합니다.
2. 응답 header에 `X-Trace-Id`가 포함되는지 확인합니다.
3. 콘솔 로그에서 같은 요청 처리 로그가 같은 `traceId`로 묶여 출력되는지 확인합니다.
4. service 흐름상 다음 event가 순서대로 기록되는지 확인합니다.

```text
incident-created
→ knowledge-loaded
→ history-loaded
→ policy-resolved
→ analysis-prompt-built
→ analysis-ai-response-received
→ analysis-completed
→ action-prompt-built
→ actions-ai-response-received
→ actions-recommended
```

모든 event가 독립 span으로 보이는 것은 아닙니다. event는 현재 service span 안에 기록되는 업무 이정표입니다.

---

## 오류 흐름 확인

오류가 발생하면 “실패했다”는 결과만 보지 말고, 같은 traceId를 기준으로 요청이 어느 지점까지 진행됐는지 확인합니다.

예를 들어 분석용 ChatClient 호출에서 오류가 발생하면 다음과 같은 흐름을 기대할 수 있습니다.

```text
incident-created
→ knowledge-loaded
→ history-loaded
→ policy-resolved
→ analysis-prompt-built
→ 분석 ChatClient 호출 실패
→ analysis-ai-response-received 없음
→ 현재 analysis/respond Observation에 error 기록
→ recommendation 단계 미실행
```

수업 7의 service catch 블록은 현재 Observation/span에 error를 기록한 뒤 예외를 다시 던집니다. HTTP 오류 응답을 만드는 책임은 기존 예외 처리 구조가 유지합니다.

> 수업 7은 오류를 관찰하는 단계입니다. JSON 매핑 실패 구분, 출력 계약 검증, retry, fallback은 수업 11의 품질 통제에서 다룹니다.

---

## 다음 수업과의 연결

```text
수업 7
  → 애플리케이션 내부에서 traceId · service span · event를 연결

수업 8
  → OTLP exporter와 Langfuse를 연결해 요청 한 건의 trace tree,
    LLM generation, token usage, latency, error를 외부 UI에서 읽기

다음 흐름 통제 수업
  → Routing Workflow로 흐름 통제하기
  → Incident를 유형·심각도별 handler로 분기하기
```

수업 7에서 만든 관찰 구조가 있어야, 이후 수업에서 “어떤 요청이 어떤 처리 경로로 갔는지”, “어느 LLM 호출이 token과 시간을 많이 사용했는지”, “오류가 어디서 발생했는지”를 근거를 가지고 판단할 수 있습니다.

---

## 마무리

이 코드 스냅샷의 목적은 새로운 AI 기능을 추가하는 것이 아닙니다. 하나의 Incident 요청이 실제로 어떤 경로를 거쳤는지 읽을 수 있도록, 애플리케이션 내부의 trace · span · event 구조를 연결하는 것입니다.

수업 7에서 확인해야 할 최종 결과는 다음과 같습니다.

```text
요청 한 건
  → 현재 Micrometer traceId 생성 및 전파
  → MDC 로그와 X-Trace-Id 응답 header 연결
  → respond / analyze / recommend service span 경계 생성
  → 업무 event 기록
  → Spring AI ChatClient 호출 문맥 연결
  → 정상·오류 흐름을 traceId 기준으로 복기할 기반 확보
```

다음 수업에서는 이 기반을 Langfuse로 export하여, 코드 안에서 만든 흐름을 외부 trace 화면에서 실제로 읽어 봅니다.