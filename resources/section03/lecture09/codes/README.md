# lecture09-routing-workflow

수업 9에서 사용하는 코드는 수업 8까지의 Incident Responder에 **Routing
Workflow를 추가해, 모든 Incident가 동일한 AI 처리 흐름을 따르지 않고
요청 특성에 따라 필요한 처리 경로를 선택하도록 변경한 코드**입니다.

수업 7에서는 요청 흐름을 trace/span/event로 관찰할 수 있게 만들었고,
수업 8에서는 그 관찰 데이터를 Langfuse로 전송해 실제 요청 한 건의 실행
흐름을 읽었습니다. 수업 9에서는 이제 관찰된 선형 흐름 앞에 **결정론적인
Routing 통제 지점**을 추가합니다.

기존에는 Incident가 생성되면 항상 knowledge, history, policy를 조회하고
Analysis LLM과 Recommendation LLM을 순서대로 호출했습니다. 수업 9에서는
`severity`, `sourceSystem`, `symptoms`를 먼저 확인해 `QUICK_TRIAGE`,
`STANDARD_ANALYSIS`, `CRITICAL_ESCALATION` 중 하나를 선택하고, 선택된
Handler가 실제 처리 흐름을 실행합니다.

이 코드 스냅샷의 목적은 복잡한 자연어 분류기를 만들거나 Rule Engine을
도입하는 것이 아닙니다. **Incident의 특성으로 route를 결정하고, 그
결정이 실제 Workflow의 차이로 이어지도록 애플리케이션의 흐름을 통제하는
것**이 목적입니다.

> **흐름 통제(Flow Control)는 모든 요청을 같은 AI 파이프라인에 넣는 것이
> 아니라, 요청의 특성을 먼저 판단하고 필요한 처리 경로만 선택하도록
> 애플리케이션이 결정하는 것입니다.**

------------------------------------------------------------------------

## 이 코드 스냅샷에서 달라진 것

수업 8 코드와 비교해, 수업 9에서는 기존의 하나의 선형 처리 흐름 앞에
Routing을 추가하고 route별 실행 책임을 Handler로 분리합니다.

  -----------------------------------------------------------------------------------------
  항목                    수업 8                         수업 9 변경
  ----------------------- ------------------------------ ----------------------------------
  최상위 처리             모든 Incident가 동일한 AI      Incident 특성으로 route를 먼저
                          Workflow 실행                  결정

  Routing 판단            없음                           `IncidentRouter` 추가

  Routing 결과            없음                           `RoutingDecision(route, reason)`
                                                         추가

  실행 경로               하나의 선형 흐름               QUICK / STANDARD / CRITICAL 세
                                                         경로

  route 실행              `IncidentResponderService`가   `IncidentRouteHandler` 구현체에
                          전체 흐름 조립                 실행 위임

  Handler 선택            없음                           `IncidentRouteHandlerRegistry`가
                                                         route → Handler 연결

  공통 AI Workflow        Service 내부에서 직접 조립     `IncidentResponseWorkflow`로
                                                         분리해 STANDARD/CRITICAL에서
                                                         재사용

  QUICK                   별도 경로 없음                 knowledge/history/LLM 호출 없이
                                                         결정론적 응답

  CRITICAL                일반 Incident와 동일한 흐름    필수 escalation policy를 AI 분석
                                                         전에 검증

  API 응답                Routing 정보 없음              `route`, `routeReason` 추가

  관찰 정보               기존 trace/span/event          `incident.route`,
                                                         `incident.route.reason`,
                                                         `route-resolved` 추가

  Langfuse 확인           하나의 선형 trace 분석         QUICK / STANDARD / CRITICAL의 실제
                                                         실행 흐름 비교
  -----------------------------------------------------------------------------------------

수업 9은 수업 7\~8에서 만든 `TraceLoggingFilter`, Micrometer
Observation, OpenTelemetry, OTLP exporter, Langfuse 연결을 그대로
사용합니다. 관찰 구조를 다시 만드는 것이 아니라, 새로 추가된 Routing
결정을 기존 trace에 연결합니다.

------------------------------------------------------------------------

## 수업 9 학습 포인트

수업 9에서는 이 프로젝트를 통해 아래 내용을 확인합니다.

-   모든 Incident를 동일한 AI Workflow로 보내는 선형 흐름의 한계
-   `IncidentRouter`가 `severity`, `sourceSystem`, `symptoms`로 route를
    결정하는 방식
-   `RoutingDecision`이 선택된 `route`와 선택 이유인 `reason`을 함께
    전달하는 이유
-   명확한 CRITICAL 조건을 먼저 평가하고 QUICK을 좁게 허용하는 규칙
    우선순위
-   `allMatch(...)`를 사용해 QUICK이 허용된 지연성 symptom만 포함할 때
    선택되도록 제한하는 이유
-   **QUICK이 아니라는 것이 곧 CRITICAL이라는 뜻은 아니라는 점**
-   `IncidentRouteHandler`가 route 결정과 route 실행 책임을 분리하는
    방식
-   `IncidentRouteHandlerRegistry`가 route와 Handler를 연결하는 방식
-   QUICK은 AI Workflow를 건너뛰고, STANDARD는 기존 AI 분석 Workflow를
    재사용하는 차이
-   CRITICAL은 유효한 escalation policy를 AI 분석의 필수 사전 조건으로
    사용하는 차이
-   Postman의 `route`, `routeReason`과 Langfuse의 실제 trace를 연결해
    읽는 방법
-   규칙 기반 라우팅(Rule-based Routing)을 선택한 이유와 LLM 분류
    라우팅(LLM Classifier Routing), 혼합 라우팅(Hybrid Routing)의 적용
    경계

------------------------------------------------------------------------

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

``` text
resources/
  section03/
    lecture09/
      codes/
        README.md
        lecture09-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture09-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

------------------------------------------------------------------------

## 실행 환경

-   Java 21
-   Spring Boot 3.5.13
-   Spring AI 1.1.4
-   Gradle
-   OpenAI API Key
-   Langfuse Japan project 사전 준비 완료
-   수업 8의 OTLP / Langfuse 환경 변수 설정 유지

### 실행 전 준비

애플리케이션 실행에는 OpenAI API Key가 필요합니다.

``` bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

Langfuse에서 Routing 결과와 route별 trace를 확인하려면 수업 8에서 사용한
아래 환경 변수도 준비되어 있어야 합니다.

``` text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

실제 key, secret, Authorization 값은 코드, `application.yml`, Git,
로그에 직접 기록하지 않습니다.

------------------------------------------------------------------------

## 실행 방법

### 1) 터미널에서 실행

`lecture09-incident-responder/` 프로젝트 루트에서 아래 명령을
실행합니다.

``` bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

``` text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

애플리케이션 실행 후 OpenAI API 또는 Langfuse exporter 관련 오류가
없는지 콘솔을 먼저 확인합니다.

------------------------------------------------------------------------

## 기본 포트와 API

기본 실행 포트는 `18080`입니다.

``` text
POST http://localhost:18080/api/incidents/respond
```

### Endpoint

``` http
POST /api/incidents/respond
```

`IncidentRequest`의 기본 요청 구조는 이전 수업과 동일합니다.

``` json
{
  "sourceSystem": "order-service",
  "title": "Order API timeout increase",
  "summary": "Order requests are intermittently timing out and the error rate has increased.",
  "severity": "HIGH",
  "symptoms": [
    "order API timeout",
    "HTTP 5xx rate increased"
  ]
}
```

수업 9에서는 Routing 결과를 바로 확인할 수 있도록 `IncidentResponse`에
다음 정보가 추가됩니다.

``` text
route
routeReason
```

예:

``` json
{
  "route": "STANDARD_ANALYSIS",
  "routeReason": "default-standard"
}
```

> `route`, `routeReason`은 실습에서 Routing 결과를 쉽게 확인하기 위한
> 교육용 필드입니다. 운영 환경에서는 내부 Routing 정책을 외부 API 응답에
> 노출하지 않고 trace, log, metric에서만 관리할 수도 있습니다.

------------------------------------------------------------------------

## Routing 적용 전과 후

### 수업 8까지

모든 Incident는 동일한 선형 흐름을 사용했습니다.

``` text
Incident
  ↓
knowledge
  ↓
history
  ↓
policy
  ↓
Analysis LLM
  ↓
Recommendation LLM
  ↓
Response
```

### 수업 9 이후

Incident를 만든 뒤 가장 먼저 route를 결정합니다.

``` text
Incident
  ↓
IncidentRouter
  ↓
RoutingDecision
  ├─ route
  └─ reason
  ↓
IncidentRouteHandlerRegistry
  ↓
IncidentRouteHandler
  ├─ QUICK_TRIAGE
  ├─ STANDARD_ANALYSIS
  └─ CRITICAL_ESCALATION
```

최상위 `IncidentResponderService`의 책임도 다음처럼 달라집니다.

``` text
수업 8까지

Incident 생성
→ knowledge
→ history
→ policy
→ analysis
→ recommendation
→ response
```

``` text
수업 9

Incident 생성
→ route 결정
→ route 관찰
→ Registry에서 Handler 선택
→ Handler에 실행 위임
```

핵심은 클래스 수가 늘어난 것이 아니라 **route를 판단하는 책임과 선택된
route를 실행하는 책임이 분리되었다는 점**입니다.

------------------------------------------------------------------------

## Routing 정책

수업 9의 Routing 규칙은 아래 순서로 평가합니다.

  -----------------------------------------------------------------------------------------------
               우선순위 조건                       Route                   Reason
  --------------------- -------------------------- ----------------------- ----------------------
                      1 `severity == CRITICAL`     `CRITICAL_ESCALATION`   `severity-critical`

                      2 `payment-service` +        `CRITICAL_ESCALATION`   `payment-failure`
                        payment failure                                    

                      3 customer data loss /       `CRITICAL_ESCALATION`   `customer-data-loss`
                        corruption / breach                                

                      4 LOW +                      `QUICK_TRIAGE`          `notification-delay`
                        `notification-service` +                           
                        delay-only symptoms                                

                      5 그 외                      `STANDARD_ANALYSIS`     `default-standard`
  -----------------------------------------------------------------------------------------------

이번 구현에서는 별도의 priority 숫자나 Rule Object Framework를 만들지
않습니다.

``` text
강한 CRITICAL 조건
→ 먼저 평가

제한적인 QUICK 조건
→ 뒤에서 평가

어느 특수 조건에도 해당하지 않음
→ STANDARD
```

즉 `IncidentRouter.route(...)`의 코드 순서 자체가 현재 Routing 정책의
우선순위를 표현합니다.

------------------------------------------------------------------------

## QUICK_TRIAGE를 좁게 허용하는 이유

QUICK은 단순히 `severity == LOW`라고 선택하지 않습니다.

``` text
LOW
AND
sourceSystem == notification-service
AND
symptoms가 비어 있지 않음
AND
모든 symptom이 delay / latency / queue lag 계열
```

핵심 조건은 다음과 같습니다.

``` java
return symptoms.stream()
        .allMatch(symptom ->
                symptom.contains("delay")
                        || symptom.contains("latency")
                        || symptom.contains("queue lag"));
```

`allMatch(...)`를 사용하는 이유는 symptom 중 하나만 delay라고 해서
QUICK으로 보내지 않기 위해서입니다.

예를 들어:

``` text
notification delivery delay
queue lag
```

는 모든 symptom이 허용된 지연성 증상이므로 QUICK 후보가 됩니다.

반면:

``` text
notification delivery delay
message delivery failure
```

는 두 번째 symptom이 QUICK 허용 조건에 맞지 않으므로 `allMatch(...)`가
`false`가 됩니다.

이 경우 `message delivery failure`가 별도의 CRITICAL 조건에도 해당하지
않으면 최종적으로 `STANDARD_ANALYSIS`로 갑니다.

또 다음과 같은 요청은:

``` text
notification delivery delay
customer data loss
```

QUICK 조건에서 CRITICAL로 바뀌는 것이 아닙니다. `customer data loss`는
Router 앞쪽의 더 높은 우선순위 규칙에서 먼저 감지되어
`CRITICAL_ESCALATION`으로 분류됩니다.

> **QUICK이 아니라는 것이 곧 CRITICAL이라는 뜻은 아닙니다.**

또한 `allMatch(...)`는 빈 Stream에서 `true`가 될 수 있으므로, QUICK
조건에서는 먼저 다음 방어 조건을 확인합니다.

``` java
if (symptoms.isEmpty()) {
    return false;
}
```

이 조건은 symptom이 하나도 없는데 QUICK으로 잘못 허용되는 상황을
막습니다.

------------------------------------------------------------------------

## 왜 단순 규칙 기반 Routing을 사용하는가

Routing은 크게 다음 접근으로 나눌 수 있습니다.

  -----------------------------------------------------------------------
  방식              판단 주체         장점              주요 통제 문제
  ----------------- ----------------- ----------------- -----------------
  규칙 기반 라우팅  Java 코드         결정론적, 빠름,   규칙 증가 시 관리
  (Rule-based                         테스트 쉬움       복잡도
  Routing)                                              

  LLM 분류 라우팅   LLM               자유로운 자연어   비용, latency,
  (LLM Classifier                     의미 해석         오분류, 출력 검증
  Routing)                                              

  혼합 라우팅       Java + LLM        명확한 조건과     구조와 테스트
  (Hybrid Routing)                    의미 해석을 결합  복잡도 증가
  -----------------------------------------------------------------------

현재 route 규칙은 LLM의 자유로운 추론이 필요한 문제가 아닙니다.

``` text
severity == CRITICAL
payment-service + payment failure
customer data loss
LOW notification delay-only
```

처럼 애플리케이션이 명시적으로 판단할 수 있으므로 Java 규칙을
사용합니다.

> **LLM을 사용하지 않는 것이 목적이 아니라, 결정론적으로 판단할 수 있는
> 문제를 굳이 확률적인 판단으로 바꾸지 않는 것이 중요합니다.**

현재 코드의 `contains(...)` 기반 문자열 비교 역시 운영 환경의 완성된
자연어 classifier를 의미하지 않습니다. 이번 수업에서 Routing 구조를
단순하게 보여 주기 위한 선택입니다.

실제 운영 환경에서는 가능하면 다음처럼 더 구조화된 입력을 먼저 검토하는
것이 좋습니다.

``` text
incident category
alert code
event type
typed service identifier
normalized signal
```

자유로운 자연어 의미 해석이 실제로 필요한 경우에는 LLM Classifier 또는
Hybrid Routing으로 확장할 수 있습니다.

> 이번 수업의 목표는 keyword classifier를 완성하는 것이 아니라,
> **route를 선택하고 실제 workflow를 분기하는 통제 구조를 만드는
> 것**입니다.

------------------------------------------------------------------------

## 핵심 코드 위치

수업 9에서 새로 추가하거나 중요하게 변경되는 파일은 아래와 같습니다.

  ----------------------------------------------------------------------------------------------------------------
  파일                                                                         수업 9에서의 책임
  ---------------------------------------------------------------------------- -----------------------------------
  `incident/application/routing/IncidentRoute.java`                            선택 가능한 QUICK / STANDARD /
                                                                               CRITICAL route 정의

  `incident/application/routing/RoutingDecision.java`                          선택된 route와 reason 전달

  `incident/application/routing/IncidentRouter.java`                           Incident 특성으로 route 결정

  `incident/application/routing/IncidentRouteHandler.java`                     route별 실행의 공통 계약

  `incident/application/routing/IncidentRouteHandlerRegistry.java`             route와 Handler 연결 및 누락/중복
                                                                               구성 방어

  `incident/application/routing/QuickTriageIncidentRouteHandler.java`          QUICK의 결정론적 경량 처리

  `incident/application/routing/StandardAnalysisIncidentRouteHandler.java`     STANDARD AI Workflow 실행

  `incident/application/routing/CriticalEscalationIncidentRouteHandler.java`   CRITICAL Workflow 실행

  `incident/application/IncidentResponseWorkflow.java`                         STANDARD/CRITICAL의 공통 AI
                                                                               분석·추천 로직 재사용

  `incident/application/IncidentResponderService.java`                         route 결정 → 관찰 → Handler 선택 →
                                                                               실행 위임

  `incident/dto/IncidentResponse.java`                                         `route`, `routeReason` 응답 정보
                                                                               추가

  `observability/AiObservationSupport.java`                                    route/reason attribute와
                                                                               `route-resolved` event 기록

  `observability/IncidentObservationEvents.java`                               Routing 및 CRITICAL policy 관련
                                                                               event 이름 관리
  ----------------------------------------------------------------------------------------------------------------

------------------------------------------------------------------------

## `IncidentRouter`

`IncidentRouter`는 Routing 판단만 담당합니다.

``` text
IncidentRouter가 하는 일
  → severity 확인
  → sourceSystem 확인
  → symptoms 확인
  → route + reason 결정
```

``` text
IncidentRouter가 하지 않는 일
  → LLM 호출
  → runbook 조회
  → history 조회
  → Prompt 생성
  → 액션 추천
```

대표 구조는 다음과 같습니다.

``` java
public RoutingDecision route(Incident incident) {
    if (incident.severity() == IncidentSeverity.CRITICAL) {
        return new RoutingDecision(
                IncidentRoute.CRITICAL_ESCALATION,
                "severity-critical"
        );
    }

    if (isPaymentFailure(incident)) {
        return new RoutingDecision(
                IncidentRoute.CRITICAL_ESCALATION,
                "payment-failure"
        );
    }

    if (containsCustomerDataLossSignal(incident)) {
        return new RoutingDecision(
                IncidentRoute.CRITICAL_ESCALATION,
                "customer-data-loss"
        );
    }

    if (isNotificationDelayOnly(incident)) {
        return new RoutingDecision(
                IncidentRoute.QUICK_TRIAGE,
                "notification-delay"
        );
    }

    return new RoutingDecision(
            IncidentRoute.STANDARD_ANALYSIS,
            "default-standard"
    );
}
```

------------------------------------------------------------------------

## `RoutingDecision`

Routing 결과는 단순히 route 하나만 반환하지 않습니다.

``` java
public record RoutingDecision(
        IncidentRoute route,
        String reason
) {
}
```

예:

``` text
route  = CRITICAL_ESCALATION
reason = payment-failure
```

`reason`은 자유로운 자연어 설명이 아니라 **어떤 규칙이 route를
선택했는지 식별하는 안정적인 이름**입니다.

``` text
severity-critical
payment-failure
customer-data-loss
notification-delay
default-standard
```

이 값은 다음 세 지점을 연결합니다.

``` text
Java 코드의 Routing 판단
→ Postman 응답
→ Langfuse trace attribute
```

------------------------------------------------------------------------

## Handler와 Registry

Router는 **어디로 보낼지** 결정하고, Handler는 **선택된 경로에서 무엇을
실행할지** 담당합니다.

``` java
public interface IncidentRouteHandler {

    IncidentRoute route();

    IncidentResponse handle(
            Incident incident,
            RoutingDecision decision
    );
}
```

각 Handler는 자신이 담당하는 route를 선언합니다.

``` text
QuickTriageIncidentRouteHandler
→ QUICK_TRIAGE

StandardAnalysisIncidentRouteHandler
→ STANDARD_ANALYSIS

CriticalEscalationIncidentRouteHandler
→ CRITICAL_ESCALATION
```

`IncidentRouteHandlerRegistry`는 `RoutingDecision.route`에 맞는
Handler를 찾습니다.

``` text
RoutingDecision.route
        ↓
IncidentRouteHandlerRegistry
        ↓
IncidentRouteHandler
```

Registry는 단순히 `switch`를 `Map`으로 바꾸는 역할만 하는 것이 아닙니다.
다음과 같은 잘못된 구성을 명확하게 실패시킵니다.

``` text
같은 route에 Handler가 두 개 등록됨
→ duplicate registration failure

선택된 route의 Handler가 없음
→ missing registration failure
```

따라서 `IncidentResponderService`는 QUICK, STANDARD, CRITICAL의 구체
Handler 클래스를 직접 알 필요가 없습니다.

------------------------------------------------------------------------

## Route별 실제 Workflow

Routing은 route 이름을 붙이는 것으로 끝나지 않습니다. 선택된 route에
따라 실제 실행 흐름이 달라집니다.

### QUICK_TRIAGE

``` text
Incident
  ↓
QUICK_TRIAGE
  ↓
결정론적인 간단한 triage 응답
```

LLM 호출:

``` text
0회
```

QUICK은 knowledge/history 조회와 Analysis/Recommendation LLM 호출을
생략합니다.

### STANDARD_ANALYSIS

``` text
Incident
  ↓
STANDARD_ANALYSIS
  ↓
knowledge
  ↓
history
  ↓
policy
  ↓
Analysis LLM
  ↓
Recommendation LLM
  ↓
Response
```

정상 실행 기준 LLM 호출:

``` text
2회
```

STANDARD는 수업 8까지의 기존 AI 분석·추천 흐름을 재사용합니다.

### CRITICAL_ESCALATION

CRITICAL은 AI 분석 전에 **필수 escalation policy**를 먼저 확인합니다.

``` text
Incident
  ↓
CRITICAL_ESCALATION
  ↓
required escalation policy
  ├─ 없음
  │    ↓
  │  Workflow 중단
  │    ↓
  │  LLM 0회
  │
  └─ 있음
       ↓
     knowledge
       ↓
     history
       ↓
     Analysis LLM
       ↓
     Recommendation LLM
       ↓
     Response
```

정상 실행 기준 LLM 호출:

``` text
2회
```

CRITICAL을 STANDARD와 다르게 만들기 위해 LLM 호출을 일부러 추가하지
않습니다.

실제 차이는 다음과 같습니다.

``` text
STANDARD
→ 일반 AI 분석 Workflow

CRITICAL
→ required escalation policy 검증
→ 검증 성공 시 AI 분석 Workflow
→ 검증 실패 시 AI 분석 전에 중단
```

> **CRITICAL Incident는 유효한 escalation policy가 존재해야만 AI 분석
> 단계로 진행할 수 있습니다.**

이 검증은 관리자 승인이나 실제 시스템 실행을 의미하지 않습니다.

``` text
포함
→ escalation policy 존재 여부 검증
→ 없으면 Workflow 중단

포함하지 않음
→ 관리자 승인
→ 승인 대기
→ Tool 실행
→ restart / rollback / scale
→ 자동 복구
```

------------------------------------------------------------------------

## `IncidentResponseWorkflow`

STANDARD와 CRITICAL은 상당 부분 같은 AI 처리 로직을 사용하므로
`IncidentResponseWorkflow`에서 공통 로직을 재사용합니다.

``` text
StandardAnalysisIncidentRouteHandler
        ─┐
         ├→ IncidentResponseWorkflow
CriticalEscalationIncidentRouteHandler
        ─┘
```

현재 구조에서는 STANDARD와 CRITICAL이 **같은 Workflow의 두 실행 변형**에
가깝기 때문에 하나의 클래스에 `executeStandard(...)`,
`executeCritical(...)` 진입점을 두고 공통 로직을 private 메서드로
재사용합니다.

향후 route별 Workflow가 더 많아지고 공통 단계와 개별 단계가 반복적으로
분리된다면 다음과 같은 설계도 검토할 수 있습니다.

``` text
Template Method + 상속
또는
별도 Workflow + Composition
```

하지만 이번 수업에서는 Workflow 상속 구조 자체가 학습 목표가 아니므로
현재 복잡도에 맞는 단순한 구조를 유지합니다.

------------------------------------------------------------------------

## `IncidentResponderService`의 최종 흐름

수업 9에서 `IncidentResponderService`는 전체 AI Workflow를 직접 실행하지
않습니다.

핵심 흐름은 다음 네 단계입니다.

``` java
RoutingDecision decision =
        incidentRouter.route(incident);

observationSupport.markRouteResolved(
        incident.incidentId().toString(),
        decision.route().name(),
        decision.reason()
);

IncidentRouteHandler handler =
        handlerRegistry.getRequiredHandler(
                decision.route()
        );

return handler.handle(
        incident,
        decision
);
```

이를 실행 흐름으로 표현하면 다음과 같습니다.

``` text
Incident 생성
  ↓
IncidentRouter
  ↓
RoutingDecision
  ↓
route / reason 관찰
  ↓
IncidentRouteHandlerRegistry
  ↓
선택된 Handler
  ↓
route별 실제 Workflow
```

> `incidentRouter.route(...)`가 수업 9에서 추가된 핵심 **Flow Control
> Point**입니다.

------------------------------------------------------------------------

## Routing Observation

Routing 결과는 Postman 응답뿐 아니라 기존 Observation에도 기록합니다.

``` text
incident.route
incident.route.reason
event: route-resolved
```

route 값은 제한된 세 값입니다.

``` text
QUICK_TRIAGE
STANDARD_ANALYSIS
CRITICAL_ESCALATION
```

reason도 제한된 식별자를 사용합니다.

``` text
severity-critical
payment-failure
customer-data-loss
notification-delay
default-standard
```

`incident.id`처럼 요청마다 달라지는 값은 high cardinality지만, route와
reason은 제한된 값이므로 low cardinality attribute로 관리할 수 있습니다.

자유로운 symptom 전체나 Prompt 전체를 route attribute에 넣지 않습니다.

attribute와 event의 역할은 다음처럼 구분합니다.

``` text
attribute
→ 어떤 결정이 내려졌는가?

event
→ 그 결정이 언제 발생했는가?
```

------------------------------------------------------------------------

## Postman 검증

### 1) QUICK_TRIAGE

``` json
{
  "sourceSystem": "notification-service",
  "title": "Notification delivery delay",
  "summary": "Push notifications are being delivered later than usual.",
  "severity": "LOW",
  "symptoms": [
    "notification delivery delay",
    "queue lag"
  ]
}
```

예상:

``` text
route       = QUICK_TRIAGE
routeReason = notification-delay
LLM 호출    = 0회
```

### 2) STANDARD_ANALYSIS

``` json
{
  "sourceSystem": "order-service",
  "title": "Order API timeout increase",
  "summary": "Order requests are intermittently timing out and the error rate has increased.",
  "severity": "HIGH",
  "symptoms": [
    "order API timeout",
    "HTTP 5xx rate increased"
  ]
}
```

예상:

``` text
route       = STANDARD_ANALYSIS
routeReason = default-standard
LLM 호출    = 2회
```

### 3) CRITICAL_ESCALATION --- severity

``` json
{
  "sourceSystem": "order-service",
  "title": "Critical order processing outage",
  "summary": "Order processing is unavailable for most customers.",
  "severity": "CRITICAL",
  "symptoms": [
    "order API unavailable",
    "HTTP 500 errors across order endpoints"
  ]
}
```

예상:

``` text
route       = CRITICAL_ESCALATION
routeReason = severity-critical
```

유효한 escalation policy가 존재하면 Analysis/Recommendation LLM을
실행합니다.

### 4) CRITICAL_ESCALATION --- payment failure

``` json
{
  "sourceSystem": "payment-service",
  "title": "Payment authorization failures",
  "summary": "Customers are unable to complete payments because authorization requests are failing.",
  "severity": "HIGH",
  "symptoms": [
    "payment authorization failure",
    "transaction failure rate increased"
  ]
}
```

예상:

``` text
route       = CRITICAL_ESCALATION
routeReason = payment-failure
```

severity만이 아니라 sourceSystem과 symptom의 명확한 업무 신호도 route를
바꿀 수 있습니다.

### 5) QUICK 경계 --- delay + 일반 미허용 symptom

``` json
{
  "sourceSystem": "notification-service",
  "title": "Notification delivery issue",
  "summary": "Notification delivery is delayed and some messages are failing.",
  "severity": "LOW",
  "symptoms": [
    "notification delivery delay",
    "message delivery failure"
  ]
}
```

예상:

``` text
route       = STANDARD_ANALYSIS
routeReason = default-standard
```

delay가 포함되어 있어도 모든 symptom이 QUICK 허용 조건을 만족하지
않으므로 QUICK이 아닙니다. 동시에 별도의 CRITICAL 조건에도 해당하지
않으므로 STANDARD가 선택됩니다.

### 6) Routing 우선순위 --- delay + customer data loss

``` json
{
  "sourceSystem": "notification-service",
  "title": "Notification delay with possible data loss",
  "summary": "Notification processing is delayed and some customer notification data may have been lost.",
  "severity": "LOW",
  "symptoms": [
    "notification delivery delay",
    "customer data loss"
  ]
}
```

예상:

``` text
route       = CRITICAL_ESCALATION
routeReason = customer-data-loss
```

`customer data loss` 규칙이 QUICK보다 먼저 평가되므로 LOW notification
요청이라도 CRITICAL로 처리됩니다.

------------------------------------------------------------------------

## Langfuse에서 확인할 구조

Postman에서는 Routing **결과**를 확인하고, Langfuse에서는 그 결정이 실제
**실행 흐름**을 바꿨는지 확인합니다.

### QUICK

``` text
respond-incident
  └─ route-quick-triage
```

확인할 핵심:

``` text
analyze-incident 없음
recommend-actions 없음
LLM generation 없음
```

### STANDARD

``` text
respond-incident
  └─ route-standard-analysis
       ├─ analyze-incident
       │    └─ LLM generation
       └─ recommend-actions
            └─ LLM generation
```

### CRITICAL --- policy 검증 성공

``` text
respond-incident
  └─ route-critical-escalation
       ├─ event: critical-escalation-policy-resolved
       ├─ analyze-incident
       │    └─ LLM generation
       └─ recommend-actions
            └─ LLM generation
```

`critical-escalation-policy-resolved`는 별도의 child span이 아니라
**필수 escalation policy 검증이 성공했음을 나타내는 event**입니다.

STANDARD와 CRITICAL은 정상 실행에서 모두 두 번의 LLM 호출이 보일 수
있습니다. 차이는 LLM 호출 횟수가 아니라 CRITICAL이 AI 분석 전에 필수
policy 검증을 통과해야 한다는 점입니다.

------------------------------------------------------------------------

## CRITICAL policy가 없을 때

CRITICAL route에서 escalation policy가 `null` 또는 blank라면 다음 예외로
Workflow를 중단합니다.

``` text
Critical incident requires an escalation policy.
```

실행 흐름:

``` text
Incident
  ↓
CRITICAL_ESCALATION
  ↓
resolveRequiredCriticalPolicy
  ↓
policy 없음
  ↓
IllegalStateException
  ↓
Analysis LLM 미실행
  ↓
Recommendation LLM 미실행
```

예상 LLM 호출:

``` text
0회
```

강의 실습 영상에서는 production adapter를 일부러 변경해 이 실패를
재현하지 않습니다. 이 동작은 코드의 사전 조건과 테스트에서 검증할 수
있습니다.

------------------------------------------------------------------------

## 현재 확인하는 범위

수업 9에서 확인하는 범위는 다음과 같습니다.

``` text
- Routing 적용 전/후 전체 클래스 구조
- IncidentRoute / RoutingDecision
- severity / sourceSystem / symptoms 기반 IncidentRouter
- Routing 규칙 우선순위
- QUICK의 좁은 allMatch 조건
- route → Handler 연결
- QUICK / STANDARD / CRITICAL의 실제 실행 차이
- CRITICAL required escalation policy
- API 응답의 route / routeReason
- trace의 incident.route / incident.route.reason
- QUICK / STANDARD / CRITICAL Langfuse trace 비교
```

이번 수업에서 다루지 않는 범위는 다음과 같습니다.

``` text
- LLM Classifier 실제 구현
- Hybrid Routing 실제 구현
- Drools / Decision Table / 외부 Rule Engine
- route별 모델 선택
- Prompt/RAG Context 크기 제한
- token budget 구현
- Evaluator
- output quality validation
- Tool 실행
- 관리자 승인
- 승인 후 workflow 재개
- restart / rollback / scale
- 자동 복구
```

------------------------------------------------------------------------

## 정상 흐름 확인

애플리케이션 실행 후 아래 항목을 순서대로 확인합니다.

1.  애플리케이션이 정상 기동되는지 확인합니다.
2.  Langfuse exporter 관련 오류가 없는지 콘솔에서 확인합니다.
3.  QUICK 요청을 보내 `QUICK_TRIAGE / notification-delay`가 반환되는지
    확인합니다.
4.  STANDARD 요청을 보내 `STANDARD_ANALYSIS / default-standard`가
    반환되는지 확인합니다.
5.  CRITICAL severity 요청을 보내
    `CRITICAL_ESCALATION / severity-critical`이 반환되는지 확인합니다.
6.  HIGH payment failure 요청이
    `CRITICAL_ESCALATION / payment-failure`로 분기되는지 확인합니다.
7.  LOW notification delay에 QUICK 미허용 일반 symptom을 섞었을 때
    STANDARD로 가는지 확인합니다.
8.  LOW notification delay에 `customer data loss`를 섞었을 때 CRITICAL로
    가는지 확인합니다.
9.  Langfuse에서 QUICK trace에 Analysis/Recommendation LLM generation이
    없는지 확인합니다.
10. STANDARD trace에 Analysis/Recommendation LLM generation이 존재하는지
    확인합니다.
11. CRITICAL trace에서 critical route와 policy 검증 성공 event, 이후 AI
    분석 흐름을 확인합니다.
12. route와 reason이 기존 Observation에 기록되는지 확인합니다.

------------------------------------------------------------------------

## 설계 확장 메모

### Rule이 많아질 때

현재는 규칙이 적고 수업 목표가 Routing 구조이므로 `IncidentRouter` 한
곳에 명시적인 조건을 둡니다.

향후 규칙이 크게 늘어나거나 정책 변경 주체가 개발자가 아니게 되면 다음
구조를 검토할 수 있습니다.

``` text
단순 IncidentRouter
  ↓
Rule Object
  ↓
정책 테이블 / YAML / DB
  ↓
Decision Table
  ↓
Rule Engine
```

이번 수업에서 Drools나 별도 Rule Engine을 사용하지 않는 이유는
기술적으로 사용할 수 없어서가 아니라, 현재 문제에 비해 불필요한 복잡도를
추가하기 때문입니다.

### Workflow 종류가 많아질 때

현재 STANDARD와 CRITICAL은 같은 AI 분석 Workflow의 변형이므로 하나의
`IncidentResponseWorkflow`에서 공통 로직을 재사용합니다.

향후 route별 Workflow가 많아지고 공통 실행 골격과 개별 단계가 반복된다면
다음을 검토할 수 있습니다.

``` text
Template Method Pattern
또는
별도 Workflow + Composition
```

현재 구조는 향후 확장이 불가능해서가 아니라 **현재 복잡도와 수업 목표에
맞게 의도적으로 단순하게 유지한 구조**입니다.

------------------------------------------------------------------------

## 다음 수업과의 연결

``` text
수업 7
→ 요청 흐름을 trace/span/event로 보이게 만든다.

수업 8
→ Langfuse에서 실제 요청 한 건의 흐름을 읽는다.

수업 9
→ Incident 특성에 따라 실행할 경로를 결정한다.

수업 10
→ 선택된 AI 경로 안에서 LLM에 전달할 Context의 양을 제한한다.
```

Routing은 일부 요청이 QUICK으로 빠지면서 결과적으로 LLM 호출을 줄일 수
있습니다. 하지만 이것이 수업 9의 직접적인 목표는 아닙니다.

Routing이 해결하는 질문은:

> **이 요청은 어느 경로로 흘러야 하는가?**

다음 비용 통제 수업이 해결하는 질문은:

> **선택된 AI 경로 안에서 LLM에 얼마나 많은 정보를 전달하도록 허용할
> 것인가?**

따라서 Routing만 추가했다고 STANDARD/CRITICAL 경로의 runbook, history,
Prompt 크기가 자동으로 제한되는 것은 아닙니다.

------------------------------------------------------------------------

## 마무리

이 코드 스냅샷의 목적은 Incident를 세 가지 이름으로 분류하는 것이
아닙니다.

기존에는:

``` text
모든 Incident
→ 동일 AI Workflow
```

였다면 수업 9 이후에는:

``` text
Incident 특성
→ RoutingDecision
→ route별 Handler
→ 필요한 Workflow만 실행
```

으로 바뀝니다.

수업 9에서 확인해야 할 최종 결과는 다음과 같습니다.

``` text
LOW notification delay-only
→ QUICK_TRIAGE
→ AI Workflow 미실행
→ LLM 0회

일반 Incident
→ STANDARD_ANALYSIS
→ 기존 Analysis / Recommendation Workflow
→ LLM 2회

CRITICAL 또는 명확한 중요 신호
→ CRITICAL_ESCALATION
→ required escalation policy 검증
   ├─ 정책 있음 → Analysis / Recommendation
   └─ 정책 없음 → AI 분석 전에 중단
```

> **Routing은 분류가 아니라 실행 흐름 선택입니다.**
