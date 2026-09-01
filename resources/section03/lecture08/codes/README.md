# lecture08-langfuse-trace-reading

수업 8에서 사용하는 코드는 수업 7의 Incident Responder에 **OTLP exporter를 추가하고, 애플리케이션 내부에서 만들어진 관찰 데이터를 Langfuse로 전송할 수 있게 구성한 코드**입니다.

수업 7에서는 `TraceLoggingFilter`, `@Observed`, `AiObservationSupport`를 통해 요청 한 건의 trace, service span, 업무 event를 애플리케이션 내부에 연결했습니다. 수업 8에서는 이미 만들어진 trace/span을 OTLP로 Langfuse Japan에 전송하고, Langfuse UI에서 요청 한 건의 흐름과 LLM 호출 정보를 읽습니다.

이 스냅샷의 목적은 Langfuse 기능을 넓게 소개하거나 Prompt를 줄이는 코드를 구현하는 것이 아닙니다. **하나의 Incident 요청이 어떤 service span과 LLM generation을 거쳤는지, 모델 호출별 token usage와 latency가 어떤지, 오류가 발생하면 어느 지점에서 흐름이 중단되는지를 외부 trace tree에서 읽는 것**입니다.

> Langfuse 가입, Japan project 생성, API key 발급, Basic Authorization 생성, 운영체제 환경 변수 등록은 별도 사전 준비 가이드를 먼저 완료해야 합니다. 이 README는 사전 준비가 끝난 상태에서 애플리케이션에 추가된 코드와 Langfuse에서 확인할 결과에 집중합니다.

---

## 이 코드 스냅샷에서 달라진 것

수업 7 코드와 비교해, 수업 8에서는 이미 만들어진 trace/span을 Langfuse로 전송하기 위한 의존성과 설정을 추가합니다.

| 항목 | 수업 7 | 수업 8 변경 |
|---|---|---|
| 관찰 구조 | 앱 내부에서 trace, service span, event를 생성·연결 | 수업 7의 관찰 구조를 그대로 유지 |
| OpenTelemetry 연결 | `micrometer-tracing-bridge-otel`로 Micrometer tracing과 OTel 구현 연결 | 기존 bridge 유지 |
| 외부 전송 | 외부 trace backend로 전송하지 않음 | `opentelemetry-exporter-otlp`를 runtime 의존성으로 추가 |
| OTLP endpoint | 없음 | `LANGFUSE_OTLP_TRACES_ENDPOINT` 환경 변수로 Langfuse OTLP ingestion endpoint 연결 |
| OTLP 인증 | 없음 | `LANGFUSE_OTLP_AUTHORIZATION` 환경 변수로 Authorization header 연결 |
| Langfuse ingestion | 없음 | `x-langfuse-ingestion-version: "4"` header 추가 |
| 요청 확인 | Postman의 `X-Trace-Id`, 콘솔 MDC/event 로그 | Langfuse trace tree, service span, LLM generation, token usage, latency, error |

수업 8은 `TraceLoggingFilter`, `AiObservationSupport`, `IncidentObservationEvents`, `@Observed` service 코드의 책임을 바꾸지 않습니다. 이 구성 요소들은 수업 7에서 만든 관찰 기반이며, 수업 8의 exporter가 이 데이터를 외부 Langfuse backend로 전송합니다.

---

## 수업 8 학습 포인트

수업 8에서는 이 프로젝트를 통해 아래 내용을 확인합니다.

- OTLP exporter가 이미 생성된 OpenTelemetry trace/span을 외부 backend로 전송하는 역할
- Micrometer Observation, OpenTelemetry bridge, OTLP exporter, Langfuse의 책임 차이
- Langfuse 목록의 row 하나가 요청 전체가 아니라 HTTP 요청, service span, LLM generation 같은 작업 하나일 수 있다는 점
- row 하나를 열어 해당 작업이 속한 전체 trace tree를 읽는 방법
- `respond-incident`, `analyze-incident`, `recommend-actions` service span을 코드와 연결하는 방법
- 분석용 LLM generation과 액션 추천용 LLM generation을 구분하는 방법
- generation별 prompt token, completion token, total token을 읽는 방법
- 전체 trace, service span, generation의 latency를 비교하는 방법
- 오류 trace에서 실패 지점, 상위 span error 전파, 이후 recommendation 미실행을 확인하는 방법
- token과 latency를 병목 확정값이 아니라 다음 코드 변경을 위한 조사 신호로 사용하는 방법

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section03/
    lecture08/
      codes/
        README.md
        lecture08-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture08-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

---

## 실행 환경

- Java 21
- Spring Boot 3.5.13
- Spring AI 1.1.4
- Gradle
- OpenAI API Key
- Langfuse Japan project 사전 준비 완료

### 실행 전 준비

애플리케이션 실행에는 OpenAI API Key가 필요합니다.

```bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

Langfuse 연결을 위해서는 사전 준비 가이드에 따라 아래 환경 변수가 준비되어 있어야 합니다.

```text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

`LANGFUSE_OTLP_TRACES_ENDPOINT`는 Langfuse 웹 UI 주소가 아니라 **OTLP trace ingestion endpoint**여야 합니다. `LANGFUSE_OTLP_AUTHORIZATION`은 민감한 인증값이므로 코드, `application.yml`, Git, 로그에 직접 기록하지 않습니다.

> 강의 기준은 Langfuse Japan region입니다. Japan 프로젝트에서 발급한 key, Japan OTLP endpoint, Japan Langfuse UI를 함께 사용해야 합니다. region을 섞으면 exporter가 HTTP 401을 반환할 수 있습니다.

> 환경 변수를 새로 등록하거나 수정했다면 Terminal과 IntelliJ를 완전히 종료한 뒤 다시 시작하세요. 이미 실행 중인 프로세스는 새 환경 변수 값을 자동으로 받지 않을 수 있습니다.

---

## 실행 방법

### 1) 터미널에서 실행

`lecture08-incident-responder/` 프로젝트 루트에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

앱을 실행한 뒤에는 exporter의 endpoint 또는 인증 관련 오류가 없는지 콘솔을 먼저 확인합니다.

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

요청과 응답의 API 계약은 수업 6 baseline 및 수업 7과 동일합니다. 수업 8은 API 계약이나 Incident 처리 로직을 바꾸지 않고, 기존 관찰 데이터를 Langfuse로 전송하는 구성을 추가합니다.

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

`-i` 옵션을 사용하면 응답 header도 함께 확인할 수 있습니다.

```http
X-Trace-Id: <current-trace-id>
```

`X-Trace-Id`는 수업 7에서 추가한 조회용 응답 header입니다. Postman 응답과 콘솔 MDC 로그를 연결하는 데 사용할 수 있습니다.

> Langfuse UI에서는 `X-Trace-Id` 또는 traceId 문자열을 직접 찾아 비교하는 것을 이 수업의 검증 기준으로 삼지 않습니다. Langfuse에서는 observation row 하나를 열어 그 작업이 속한 전체 trace tree를 읽습니다.

---

## 요청 흐름과 전송 구조

수업 7에서 애플리케이션 내부에 만들어진 관찰 흐름은 다음과 같습니다.

```text
HTTP POST /api/incidents/respond
  └─ HTTP 요청 Observation
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

수업 8에서는 이 흐름을 새로 만들지 않고, 아래 경로로 Langfuse에 전송합니다.

```text
Spring Boot + Spring AI
  → Micrometer Observation / Tracing
  → micrometer-tracing-bridge-otel
  → OpenTelemetry trace / span
  → OTLP exporter
  → Langfuse Japan
```

각 구성 요소의 역할은 다음과 같습니다.

| 구성 요소 | 역할 |
|---|---|
| Micrometer Observation | Spring 애플리케이션 내부의 관찰 추상화 |
| `micrometer-tracing-bridge-otel` | Micrometer tracing과 OpenTelemetry 구현 연결 |
| OpenTelemetry | trace/span을 표준 방식으로 표현하는 생태계 |
| OTLP | trace/span을 외부 backend로 보내는 전송 규약 |
| OTLP exporter | 이미 만들어진 trace/span을 Langfuse endpoint로 전송 |
| Langfuse | 수신한 trace를 저장하고 UI에서 분석하는 backend |

> **bridge는 연결, exporter는 전송입니다.** OTLP exporter는 trace를 만드는 기능이 아니라 이미 만들어진 trace/span을 외부로 보내는 기능입니다.

---

## 핵심 코드 위치

수업 8에서 추가하거나 확인할 파일은 아래와 같습니다.

| 파일 | 수업 8에서 확인할 책임 |
|---|---|
| `build.gradle` | `opentelemetry-exporter-otlp` runtime 의존성 추가 및 수업 7 관찰성 기반 의존성 확인 |
| `src/main/resources/application.yml` | OTLP trace endpoint, Authorization header, ingestion version, sampling 설정 |

수업 8에서 새로 추가되는 핵심 변경은 `build.gradle`과 `application.yml`입니다. service 코드, `@Observed`, event 구조는 수업 7에서 만든 내용을 유지합니다.

---

## 추가된 의존성

수업 7의 관찰성 기반 의존성에 아래 exporter를 추가합니다.

```gradle
runtimeOnly 'io.opentelemetry:opentelemetry-exporter-otlp'
```

전체 관련 의존성은 다음과 같습니다.

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-aop'
    implementation 'io.micrometer:micrometer-tracing-bridge-otel'

    runtimeOnly 'io.opentelemetry:opentelemetry-exporter-otlp'
}
```

`runtimeOnly`로 선언하는 이유는 애플리케이션 코드가 exporter 클래스를 직접 호출하지 않고, Spring Boot auto-configuration이 runtime classpath에서 exporter를 발견해 구성하기 때문입니다.

특별한 호환성 요구가 없다면 exporter 버전은 직접 고정하지 않고 Spring Boot dependency management에 맡깁니다.

---

## 추가된 OTLP 설정

수업 8에서는 `application.yml`에 아래 설정을 추가하거나 유지합니다.

```yaml
spring:
  application:
    name: incident-responder

management:
  observations:
    annotations:
      enabled: true

  tracing:
    sampling:
      probability: 1.0
  # 수업 8에서 추가되는 OTLP 설정
  otlp:
    tracing:
      endpoint: ${LANGFUSE_OTLP_TRACES_ENDPOINT}
      headers:
        Authorization: ${LANGFUSE_OTLP_AUTHORIZATION}
        x-langfuse-ingestion-version: "4"
```

| 설정 | 역할 |
|---|---|
| `management.otlp.tracing.endpoint` | OTLP exporter가 trace를 보낼 Langfuse ingestion endpoint |
| `management.otlp.tracing.headers.Authorization` | OTLP 전송에 사용할 인증 header. 환경 변수로만 주입 |
| `x-langfuse-ingestion-version: "4"` | 실습에서 요청 직후 최신 trace를 찾기 위한 Langfuse v4 ingestion header |

> `LANGFUSE_OTLP_TRACES_ENDPOINT`의 실제 endpoint 경로는 사용 중인 Langfuse Japan 프로젝트의 OpenTelemetry integration 화면을 기준으로 확인하세요. 웹 UI 주소를 endpoint로 사용하면 안 됩니다.

---

## Langfuse에서 확인할 구조

정상 Incident 요청을 보낸 뒤 Langfuse에서는 아래 구조를 확인합니다.

```text
POST /api/incidents/respond
  → respond-incident
      → analyze-incident
          → 분석용 LLM generation
      → recommend-actions
          → 액션 추천용 LLM generation
```

`@Observed`의 `contextualName`이 Langfuse Name에 표시되는 현재 구성 기준으로, 코드와 UI는 다음처럼 연결됩니다.

| 코드 위치 | Langfuse에서 읽을 이름 | 업무 의미 |
|---|---|---|
| `IncidentResponderService.respond()` | `respond-incident` | Incident 요청 전체 조립 |
| `IncidentAnalysisService.analyze()` | `analyze-incident` | 장애 원인 분석 |
| `IncidentActionRecommendationService.recommend()` | `recommend-actions` | 대응 액션 추천 |

### 목록 row와 trace tree

Langfuse 목록에서 한 Incident 요청이 한 줄만 보이지 않을 수 있습니다.

```text
목록의 row 하나
  → HTTP 요청, service span, LLM generation 중 작업 하나일 수 있음

trace tree
  → Incident 요청 한 건에서 발생한 작업의 전체 연결 구조
```

따라서 `POST /api/incidents/respond`, `respond-incident`, `analyze-incident`, `recommend-actions`, LLM generation 중 어느 row를 열어도 그 작업이 속한 전체 trace tree를 확인할 수 있습니다.

> 목록의 row 개수를 요청 개수로 해석하지 않습니다. Langfuse UI에서 traceId 문자열을 직접 비교하기보다, trace tree 안에서 HTTP 요청, service span, LLM generation의 부모-자식 관계를 따라 요청 한 건을 읽습니다.

---

## Token usage 읽기

각 LLM generation에서 prompt token, completion token, total token을 확인합니다.

```text
881 prompt → 241 completion (∑ 1,122)
```

| 항목 | 의미 |
|---|---|
| Prompt token | 모델에 전달한 입력, Prompt와 포함된 문맥이 사용한 token 수 |
| Completion token | 모델이 생성한 응답이 사용한 output token 수 |
| Total token | 해당 모델 호출의 input + output token 합계 |

Incident Responder는 분석과 액션 추천을 위해 모델을 두 번 호출하므로, 요청 전체 합계만 보지 않고 generation별로 나누어 읽습니다.

```text
분석 generation의 input token이 큼
  → runbook snippets, similar incidents, escalation policy가
    analysis Prompt에 과도하게 포함됐는지 조사

추천 generation의 input token이 큼
  → analysis result 전체 또는 policy 문맥이
    action Prompt에 중복·과도하게 포함됐는지 조사
```

> 수업 8에서는 token을 읽습니다. Prompt/RAG 문맥의 길이·개수 제한, 요약, token cap 구현과 전후 비교는 후속 비용 통제 수업에서 다룹니다.

---

## Latency 읽기

Latency는 아래 순서로 비교합니다.

```text
1. 전체 trace duration
2. respond-incident service span duration
3. analyze-incident service span duration
4. 분석 LLM generation duration
5. recommend-actions service span duration
6. 액션 추천 LLM generation duration
7. service span과 하위 generation duration의 차이
```

해석 예시는 다음과 같습니다.

```text
analysis service span ≈ 분석 generation duration
  → 분석 service 시간의 대부분이 모델 호출일 가능성

analysis service span > 분석 generation duration
  → Prompt 조립, runbook/history 조회, 응답 mapping 같은 코드 경로도 조사

recommendation generation의 input token이 크고 duration도 큼
  → 분석 결과 또는 정책 문맥이 두 번째 Prompt를 키웠을 가능성 조사
```

> Latency는 원인을 확정하는 값이 아니라, 어디를 먼저 조사할지 좁혀 주는 신호입니다. 모델 provider 네트워크, 모델 처리 시간, 출력 길이는 요청마다 달라질 수 있으므로 trace 한 건만으로 병목을 확정하지 않습니다.

---

## 오류 흐름 읽기

오류 trace에서는 “실패했다”는 결과보다 다음을 확인합니다.

- 요청은 어디까지 정상 진행됐는가?
- 어느 service 또는 LLM generation에서 실패했는가?
- 상위 `analyze-incident`, `respond-incident`에도 error가 전파됐는가?
- 이후 `recommend-actions`와 액션 추천 generation은 왜 실행되지 않았는가?

예를 들어 model provider API key가 invalid인 경우, 다음과 같은 흐름을 기대할 수 있습니다.

```text
incident-created
→ knowledge-loaded
→ history-loaded
→ policy-resolved
→ analysis-prompt-built
→ ChatClient 호출 실패
→ analysis-ai-response-received 없음
→ analyze service span error
→ respond service span error
→ recommendation 미실행
```

수업 8은 에러를 읽는 수업입니다. retry, fallback, mapping failure, contract failure 처리는 이 코드 스냅샷에 추가하지 않습니다.

오류 실습 후에는 정상 model provider API key를 복구하고 애플리케이션을 재시작한 뒤 정상 요청이 다시 성공하는지 확인합니다.

---

## 현재 확인하는 범위

현재 Langfuse 연결에서 기본적으로 확인하는 범위는 다음과 같습니다.

```text
- HTTP 요청 observation
- respond-incident, analyze-incident, recommend-actions service span
- 분석 및 액션 추천 LLM generation
- generation별 token usage
- trace/span/generation latency
- error 흐름
```

---

## 정상 흐름 확인

정상 Incident 요청을 보낸 뒤 아래 항목을 확인합니다.

1. 애플리케이션이 정상 기동되고 `/actuator/health`가 `UP`인지 확인합니다.
2. exporter의 401 또는 endpoint 오류가 없는지 콘솔에서 확인합니다.
3. `POST /api/incidents/respond` 요청이 정상 응답을 반환하는지 확인합니다.
4. 응답 header에 `X-Trace-Id`가 포함되는지 확인합니다.
5. Langfuse Japan 목록에서 HTTP/service/LLM observation row를 확인합니다.
6. 관련 row 하나를 열어 전체 trace tree를 확인합니다.
7. `respond-incident`, `analyze-incident`, `recommend-actions` service span을 확인합니다.
8. 분석 generation과 액션 추천 generation을 각각 확인합니다.
9. 각 generation의 prompt/completion/total token usage를 확인합니다.
10. 전체 trace, service span, generation의 latency를 비교합니다.

---

## 문제 해결

### exporter에서 HTTP 401이 발생할 때

```text
Failed to export spans.
Server responded with HTTP status code 401
```

아래 순서로 확인합니다.

1. endpoint host가 Langfuse Japan region인지 확인합니다.
2. 사용한 key가 Japan 프로젝트에서 발급됐는지 확인합니다.
3. Authorization이 현재 key pair로 생성된 값인지 확인합니다.
4. key rotate 후 Authorization을 다시 만들었는지 확인합니다.
5. 환경 변수 변경 뒤 Terminal과 IntelliJ를 재시작했는지 확인합니다.

실제 key, secret, Authorization 값은 로그, 이슈, 화면 공유에 노출하지 않습니다.

### Langfuse에서 row가 여러 개 보일 때

HTTP 요청, service 실행, LLM generation이 각각 observation row로 보일 수 있습니다. row 하나가 요청 전체를 뜻하지 않으므로, 관련 row 하나를 열고 전체 trace tree에서 요청 한 건의 구조를 확인합니다.

---

## 다음 수업과의 연결

```text
수업 7
  → 애플리케이션 내부에서 traceId · service span · event를 연결

수업 8
  → OTLP exporter와 Langfuse를 연결해 요청 한 건의 trace tree,
    LLM generation, token usage, latency, error를 외부 UI에서 읽기

다음 수업
  → Routing Workflow로 흐름 통제하기
  → Incident를 유형·심각도별 handler로 분기하기
```

다음 수업에서는 모든 Incident가 같은 선형 경로를 따르는 baseline 문제를 Routing Workflow로 바꾸고, 유형·심각도별 handler로 분기하는 결정론적 흐름 통제를 구현합니다.

---

## 마무리

이 코드 스냅샷의 목적은 새로운 AI 기능을 추가하는 것이 아닙니다. 수업 7에서 애플리케이션 내부에 만든 관찰 구조를 OTLP exporter로 Langfuse에 전송하고, 요청 한 건의 흐름과 비용 신호를 외부 trace 화면에서 읽을 수 있게 만드는 것입니다.

수업 8에서 확인해야 할 최종 결과는 다음과 같습니다.

```text
요청 한 건
  → HTTP, service, LLM 작업이 trace tree로 연결됨
  → respond / analyze / recommend service span 확인
  → 분석과 액션 추천 LLM generation 구분
  → LLM generation별 token usage 확인
  → trace/service/generation latency 비교
  → error 발생 지점과 이후 미실행 단계 확인
```