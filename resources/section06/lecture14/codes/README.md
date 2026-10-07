# lecture14-common-policy-advisor

수업 14에서 사용하는 코드는 수업 13까지의 Incident Responder에 **Spring
AI Advisor를 이용한 공통 정책 컨텍스트 적용 구조를 추가한 코드**입니다.

수업 13에서는 외부 심각도를 애플리케이션 정책으로 보정하고,
`EscalationDecision`으로 에스컬레이션 정책을 구조화했으며, AI 추천에
위험 분류와 애플리케이션 정책을 적용했습니다.

하지만 Incident Responder에는 이제 하나의 LLM 호출만 존재하지 않습니다.

``` text
Analysis Generation
Action Generation
Quality Evaluation
Quality Optimization
Re-Evaluation
```

수업 13에서는 Analysis와 Action 생성 Prompt에 `EscalationDecision`을
직접 전달했습니다. 반면 Evaluator와 Optimizer는 각자의 Prompt를 구성하기
때문에 동일한 정책 컨텍스트를 전달받지 않았습니다.

LLM 호출이 늘어날 때마다 각 Prompt가 공통 정책을 직접 구성하도록 만들면
`중복 / 누락 / 불일치`가 발생할 수 있습니다.

수업 14에서는 정책을 새로 판단하지 않습니다.

**Java 애플리케이션이 이미 결정한 공통 정책 컨텍스트를 여러 LLM 호출
경계에 일관되게 적용하는 구조**를 추가합니다.

``` text
Java Application
        ↓
Policy Decision
        ↓
EscalationDecision
        ↓
ChatClientRequest Context
        ↓
IncidentPolicyAdvisor
        ↓
Common Policy Context
        ↓
Analysis / Action / Evaluator / Optimizer
```

이 코드 스냅샷의 목적은 Advisor에게 도메인 정책 판단을 맡기는 것이
아닙니다.

**정책 판단은 Java 애플리케이션이 수행하고, Advisor는 이미 결정된 정책
컨텍스트를 LLM 호출 경계에서 일관되게 적용합니다.**

------------------------------------------------------------------------

## 이 코드 스냅샷에서 달라진 것

수업 13과 비교해, 수업 14에서는 기존 위험 정책과 품질 통제 구조를
유지하면서 **공통 정책 컨텍스트를 LLM 호출에 전달하는 경계**를
변경합니다.

  ----------------------------------------------------------------------------------
  항목                    수업 13                 수업 14 변경
  ----------------------- ----------------------- ----------------------------------
  Routing                 QUICK / STANDARD /      기존 구조 유지
                          CRITICAL                

  Context Budget          LLM 입력 Context 크기   기존 구조 유지
                          제한                    

  기본 계약 검증          JSON-only + DTO 기본    기존 구조 유지
                          계약                    

  의미적 품질 통제        Evaluator--Optimizer    기존 구조 유지

  외부 severity           `requestedSeverity` /   기존 구조 유지
                          effective `severity`    

  에스컬레이션 정책 판단  Java에서                기존 구조 유지
                          `EscalationDecision`    
                          생성                    

  Action 위험 통제        위험 분류 + 중복 제거 + 기존 구조 유지
                          Java 정책               

  정책 Prompt 구성        Analysis / Action       공통 정책 결합 책임 제거
                          PromptFactory에서 직접  
                          결합                    

  정책 전달 범위          Analysis / Action 생성  Evaluator / Optimizer /
                          중심                    Re-Evaluator까지 확대

  요청별 정책 전달        일부 LLM 호출까지 전달  모든 관련 LLM 호출까지 명시적으로
                                                  전달

  LLM 호출 경계           각 호출이 Prompt를 직접 `promptWithPolicy()`로 정책
                          사용                    Context 전달

  공통 정책 적용          Prompt별 직접 구성      `IncidentPolicyAdvisor`에서
                                                  일관되게 적용

  Advisor 요청 데이터     없음                    `ChatClientRequest Context` 사용

  Advisor 등록            없음                    `defaultAdvisors(policyAdvisor)`

  정책이 없는 요청        별도 공통 처리 없음     원래 Request를 그대로 통과
  ----------------------------------------------------------------------------------

수업 14는 수업 13의 정책을 다시 설계하는 수업이 아닙니다.

**이미 결정된 정책을 여러 LLM 호출에 빠짐없이 전달하고 적용하는 책임을
하나의 호출 경계로 모으는 수업**입니다.

------------------------------------------------------------------------

## 수업 14 학습 포인트

수업 14에서는 다음 내용을 확인합니다.

-   정책을 결정하는 것과 정책 컨텍스트를 전달하는 것이 다른 책임인 이유
-   LLM 호출 지점이 늘어날수록 Prompt별 정책 결합이 중복과 누락을 만들
    수 있는 이유
-   `IncidentPolicyAdvisor`의 역할
-   Advisor가 도메인 정책을 새로 판단하지 않아야 하는 이유
-   `ChatClientRequest Context`의 의미
-   `advisorSpec.param(...)`으로 요청별 정책 데이터를 전달하는 방법
-   `request.context()`에서 `EscalationDecision`을 읽는 방법
-   `defaultAdvisors(...)`로 공통 Advisor를 등록하는 방법
-   `BaseAdvisor.before(...)`에서 Model 호출 전 Request를 변경하는 방법
-   기존 User Prompt를 보존하면서 공통 정책 컨텍스트를 추가하는 방법
-   `Prompt.augmentUserMessage(...)` 사용 시 기존 Prompt를 명시적으로
    결합해야 하는 이유
-   `request.mutate()`로 변경된 `ChatClientRequest`를 만드는 방법
-   정책 Context가 없을 때 원래 Request를 그대로 반환하는 이유
-   Analysis / Action / Evaluator / Optimizer에 동일한 정책을 전달하는
    방법
-   서비스 계층에서 `EscalationDecision`을 명시적으로 전달하는 이유
-   현재 단계에서 별도의 `IncidentAiContext` Wrapper를 추가하지 않는
    이유
-   Prompt Factory와 Advisor의 책임을 구분하는 방법
-   Prompt에 정책을 전달하는 것과 Java 정책을 실제 적용하는 것이 다른
    이유
-   Advisor 적용 후에도 기존 Incident 처리 흐름이 정상적으로 유지되는지
    확인하는 방법

------------------------------------------------------------------------

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

``` text
resources/
  section05/
    lecture14/
      codes/
        README.md
        lecture14-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

`lecture14-incident-responder/` 디렉터리가 실제 Spring Boot 애플리케이션
프로젝트 루트입니다.

------------------------------------------------------------------------

## 실행 환경

-   Java 21
-   Spring Boot 3.5.13
-   Spring AI 1.1.4
-   Gradle
-   OpenAI API Key
-   수업 8에서 구성한 Langfuse 연결 환경

### 실행 전 준비

애플리케이션 실행에는 OpenAI API Key가 필요합니다.

``` bash
export OPENAI_API_KEY=your-api-key
```

Windows 환경에서는 IntelliJ IDEA의 Run Configuration에서 환경 변수를
설정할 수 있습니다.

Langfuse 연결 환경을 유지하는 경우 수업 8에서 사용한 환경 변수도
설정합니다.

``` text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

실제 API Key와 Authorization 값은 소스코드, `application.yml`, Git
저장소 등에 직접 기록하지 않습니다.

------------------------------------------------------------------------

## 실행 방법

### 1. 터미널에서 실행

`lecture14-incident-responder/` 프로젝트 루트에서 다음 명령을
실행합니다.

``` bash
./gradlew bootRun
```

Windows에서는 다음 명령을 사용할 수 있습니다.

``` powershell
.\gradlew.bat bootRun
```

### 2. IntelliJ IDEA에서 실행

다음 메인 클래스를 실행합니다.

``` text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

애플리케이션 실행 후 콘솔에서 OpenAI API 연결 오류와 Spring Bean 생성
오류가 없는지 확인합니다.

------------------------------------------------------------------------

## 기본 포트와 API

기본 실행 포트는 `18080`입니다.

``` http
POST http://localhost:18080/api/incidents/respond
```

### 정상 요청 예시

Advisor 적용 이후에도 기존 Incident 처리 흐름이 정상적으로 동작하는지
확인하기 위해 다음 요청을 사용할 수 있습니다.

``` json
{
  "sourceSystem": "payment-service",
  "title": "Payment processing failures increased",
  "summary": "Payment requests are intermittently failing and response latency has increased.",
  "severity": "MEDIUM",
  "symptoms": [
    "payment request failures increased",
    "database connection wait time increased",
    "payment response latency increased"
  ]
}
```

`payment-service`의 `minimumSeverity`가 `HIGH`라면 수업 13에서 추가한
정책도 그대로 유지됩니다.

``` text
requestedSeverity = MEDIUM
        ↓
minimumSeverity = HIGH
        ↓
effective severity = HIGH
        ↓
EscalationDecision
        ↓
여러 LLM 호출에 공통 정책 컨텍스트 전달
```

실제 Analysis와 Action 내용, Evaluator 판단은 LLM의 확률적 동작에 따라
달라질 수 있습니다.

Postman 확인의 목적은 개별 Prompt 내부의 정책 문자열을 직접 검증하는
것이 아닙니다.

**Advisor 적용 이후에도 Incident 요청부터 분석, 추천, 품질 통제, 위험
통제, 최종 응답까지 전체 처리 흐름이 정상적으로 동작하는지 확인하는
것**이 목적입니다.

------------------------------------------------------------------------

## 수업 14에서 해결하려는 문제

### 1. 정책이 존재한다고 해서 모든 LLM 호출에 자동으로 전달되지는 않습니다.

수업 13에서는 `EscalationDecision`을 구조화하고 Analysis와 Action 생성
Prompt에서 이 정책을 직접 표현했습니다.

``` text
Analysis Generation
→ Policy O

Action Generation
→ Policy O

Quality Evaluation
→ Policy X

Quality Optimization
→ Policy X
```

수업 12에서 Evaluator--Optimizer를 추가하면서 실제 LLM 호출 지점은
다음처럼 늘어났습니다.

``` text
Analysis Generation
Analysis Evaluator
Analysis Optimizer
Analysis Re-Evaluator

Action Generation
Action Evaluator
Action Optimizer
Action Re-Evaluator
```

정책 객체가 애플리케이션에 존재하는 것과 모든 LLM 호출이 그 정책
컨텍스트를 사용하는 것은 다른 문제입니다.

### 2. 각 Prompt Factory가 공통 정책을 직접 구성하면 책임이 반복됩니다.

``` text
AnalysisPromptFactory
→ EscalationDecision을 문자열로 변환

ActionPromptFactory
→ EscalationDecision을 문자열로 변환

EvaluationPromptFactory
→ EscalationDecision을 문자열로 변환

OptimizerPromptFactory
→ EscalationDecision을 문자열로 변환
```

호출 지점이 늘어날수록 같은 정책 표현 코드의 중복, 정책 필드 변경 시
다수 Prompt 수정, 일부 Prompt의 정책 누락, Prompt별 표현 불일치가 발생할
수 있습니다.

수업 14에서는 책임을 다음처럼 분리합니다.

``` text
Prompt Factory
→ 해당 호출에 필요한 Context와 지시사항

IncidentPolicyAdvisor
→ 여러 호출에 공통으로 필요한 정책 Context
```

### 3. Advisor가 정책 판단까지 맡아서는 안 됩니다.

Advisor를 추가했다고 해서 다음 책임까지 Advisor로 이동하지 않습니다.

``` text
effectiveSeverity 계산
새로운 Escalation Policy 결정
Action riskLevel 분류
위험 Action 포함 / 제외
위험 Action 중복 제거
```

책임은 다음과 같이 구분합니다.

``` text
정책 판단
→ Java Application

정책 컨텍스트 전달
→ Advisor

최종 정책 적용
→ Java Application
```

`IncidentPolicyAdvisor`가 받는 `EscalationDecision`은 이미 Java
애플리케이션이 결정한 정책 결과입니다.

------------------------------------------------------------------------

## Prompt Factory와 Advisor의 책임

수업 14에서는 다음과 같이 책임을 나눕니다.

``` text
Analysis Prompt
→ Incident + Runbook + History + output contract

Action Prompt
→ Incident + Analysis + output contract

Evaluator Prompt
→ Incident + evaluation target + evaluation criteria

Optimizer Prompt
→ Incident + current result + evaluator feedback

            +

IncidentPolicyAdvisor
→ Common Escalation Policy Context
```

수업 13의 Analysis / Action Prompt Factory가 직접 포함하던 공통 정책
블록은 제거합니다.

이 변경의 목적은 단순히 Prompt 코드를 줄이는 것이 아닙니다.

**공통 정책 컨텍스트를 적용하는 위치를 LLM 호출 경계로 모으는 것**이
목적입니다.

------------------------------------------------------------------------

## IncidentPolicyAdvisor

`IncidentPolicyAdvisor`는 Spring AI의 `BaseAdvisor`를 구현합니다.

``` text
ChatClientRequest
    ↓
before(...)
    ↓
ChatClientRequest Context 조회
    ↓
EscalationDecision 존재?
    ├─ No → 원래 Request 반환
    └─ Yes
         ↓
    Common Policy Context 생성
         ↓
    기존 User Prompt 뒤에 추가
         ↓
    새로운 ChatClientRequest 반환
         ↓
       Model
```

핵심 구현은 다음과 같은 구조입니다.

``` java
@Component
public class IncidentPolicyAdvisor implements BaseAdvisor {

    public static final String ESCALATION_POLICY_CONTEXT_KEY =
            "incident.escalation-policy";

    @Override
    public ChatClientRequest before(
            ChatClientRequest request,
            AdvisorChain advisorChain) {

        Object value =
                request.context().get(ESCALATION_POLICY_CONTEXT_KEY);

        if (!(value instanceof EscalationDecision escalationPolicy)) {
            return request;
        }

        String policyContext = formatPolicyContext(escalationPolicy);

        String originalUserText = request.prompt()
                .getUserMessage()
                .getText();

        String augmentedUserText =
                originalUserText + "\n\n" + policyContext;

        return request.mutate()
                .prompt(
                        request.prompt()
                                .augmentUserMessage(augmentedUserText)
                )
                .build();
    }

    @Override
    public ChatClientResponse after(
            ChatClientResponse response,
            AdvisorChain advisorChain) {
        return response;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
```

수업 14에서는 `before(...)`가 핵심입니다. Model 호출 전에 Request를
확인하고, 요청별 정책이 존재하면 기존 Prompt에 공통 정책 컨텍스트를
추가합니다.

------------------------------------------------------------------------

## ChatClientRequest Context

수업 14에서 사용하는 Context는 `IncidentPolicyAdvisor` 객체 내부의 저장
공간이 아닙니다.

**현재 `ChatClientRequest`에 연결된 요청별 데이터 공간**입니다.

Gateway에서는 다음과 같이 정책 데이터를 넣습니다.

``` java
.advisors(advisorSpec -> advisorSpec
        .param(
                IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY,
                escalationPolicy
        )
)
```

이 코드는 `IncidentPolicyAdvisor` 메서드를 직접 호출하는 코드가
아닙니다. 현재 ChatClient 요청에 Advisor가 사용할 수 있는 요청별
데이터를 추가합니다.

이후 등록된 `IncidentPolicyAdvisor`는 다음과 같이 읽습니다.

``` java
request.context().get(
        IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY
)
```

``` text
Gateway
    ↓
advisorSpec.param(key, EscalationDecision)
    ↓
ChatClientRequest Context
    ↓
IncidentPolicyAdvisor.before(...)
    ↓
request.context().get(key)
```

> **Context는 Advisor의 저장 공간이 아니라, Advisor가 사용할 수 있는
> 요청별 데이터 공간입니다.**

------------------------------------------------------------------------

## AdvisorSpec과 IncidentPolicyAdvisor

다음 코드의 `advisorSpec`은 `IncidentPolicyAdvisor` 자체가 아닙니다.

``` java
.advisors(advisorSpec -> advisorSpec
        .param(
                IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY,
                escalationPolicy
        )
)
```

역할은 다음과 같이 구분합니다.

``` text
AdvisorSpec
→ 현재 ChatClient 요청의 Advisor 관련 데이터 구성

IncidentPolicyAdvisor
→ 실제 Advisor 동작 수행
```

즉,

``` text
advisorSpec.param(...)
≠ IncidentPolicyAdvisor에 직접 값 전달

advisorSpec.param(...)
→ 현재 Request에 요청별 Context 추가
```

------------------------------------------------------------------------

## defaultAdvisors로 공통 Advisor 등록하기

`IncidentPolicyAdvisor`는 `ChatClient`를 구성할 때 기본 Advisor로
등록합니다.

``` java
this.chatClient =
        builder
                .defaultAdvisors(policyAdvisor)
                .build();
```

하지만 Advisor가 등록되어 있다는 사실만으로 정책 데이터가 자동으로
생기는 것은 아닙니다.

``` text
Default Advisor 등록
        +
Request별 EscalationDecision
        ↓
IncidentPolicyAdvisor가 정책 Context 적용
```

공통 동작과 요청별 데이터를 구분하는 것이 중요합니다.

------------------------------------------------------------------------

## 기존 User Prompt를 보존하면서 정책 컨텍스트 추가하기

`Prompt.augmentUserMessage(...)`에 공통 정책 문자열만 전달하면 기존 User
Prompt 뒤에 자동으로 이어 붙는다고 가정해서는 안 됩니다.

수업 14에서는 원래 User Prompt를 먼저 읽고 명시적으로 결합합니다.

``` java
String originalUserText = request.prompt()
        .getUserMessage()
        .getText();

String augmentedUserText =
        originalUserText + "\n\n" + policyContext;
```

그 후 결합된 문자열로 새로운 Prompt를 만듭니다.

``` java
request.prompt()
        .augmentUserMessage(augmentedUserText)
```

``` text
Original User Prompt
        +
Common Policy Context
        ↓
Augmented User Prompt
```

정책을 추가하는 과정에서 원래 Incident Prompt가 사라지지 않도록 하는
것이 중요합니다.

------------------------------------------------------------------------

## request.mutate()와 새로운 Request

수업 14에서는 기존 `ChatClientRequest`를 직접 수정하는 대신 기존
Request를 기반으로 새로운 Request를 만듭니다.

``` java
return request.mutate()
        .prompt(
                request.prompt()
                        .augmentUserMessage(augmentedUserText)
        )
        .build();
```

``` text
기존 Prompt
    ↓
augmentUserMessage(...)
    ↓
새 Prompt
    ↓
request.mutate().prompt(...)
    ↓
새 ChatClientRequest
```

Advisor Chain에서도 변경된 Request가 다음 Advisor 또는 Model로
전달됩니다.

``` text
Original Request
    ↓
Advisor A
    ↓
Modified Request
    ↓
Advisor B
    ↓
Modified Request
    ↓
Model
```

------------------------------------------------------------------------

## 공통 정책 컨텍스트

`IncidentPolicyAdvisor`는 `EscalationDecision`을 LLM이 읽을 수 있는 공통
정책 컨텍스트로 표현합니다.

``` text
COMMON INCIDENT POLICY CONTEXT

Business criticality:
...

Acknowledge within:
...

Notify channels:
...

Recovery action approval required:
...

Escalation instruction:
...
```

필요한 행동 지시도 함께 포함합니다.

``` text
Treat this policy as incident-response context.

Do not claim that approval has already been granted.

Do not claim that an external system change
has already been executed.
```

여기서 중요한 점은 이 텍스트가 새로운 정책을 만드는 것이 아니라는
것입니다.

``` text
EscalationDecision
→ Java가 이미 결정한 구조화된 정책

Common Policy Context
→ 그 정책을 LLM이 읽을 수 있도록 표현한 형태
```

------------------------------------------------------------------------

## 정책 컨텍스트와 최종 정책 적용은 다릅니다.

Prompt에 정책을 전달한다고 해서 Java 애플리케이션의 위험 정책 책임이
LLM으로 이동하는 것은 아닙니다.

``` text
Prompt에 정책 전달
≠
최종 정책 적용
```

최종 Action 위험 통제는 여전히 Java 애플리케이션이 수행합니다.

``` text
LLM
    ↓
Recommendation
    ↓
Quality Control
    ↓
ActionRiskClassifier
    ↓
IncidentRiskPolicyService
    ↓
Final External Response
```

수업 14의 책임 구분은 다음과 같습니다.

``` text
정책 판단
→ Java Application

정책 컨텍스트 전달
→ Advisor

최종 정책 적용
→ Java Application
```

------------------------------------------------------------------------

## promptWithPolicy()

`IncidentChatGateway`에서는 모든 관련 LLM 호출이 같은 방식으로 정책
Context를 전달할 수 있도록 `promptWithPolicy()`를 사용합니다.

``` java
private ChatClient.ChatClientRequestSpec promptWithPolicy(
        String prompt,
        EscalationDecision escalationPolicy) {

    return chatClient
            .prompt()
            .user(prompt)
            .advisors(advisorSpec -> advisorSpec
                    .param(
                            IncidentPolicyAdvisor.ESCALATION_POLICY_CONTEXT_KEY,
                            escalationPolicy
                    )
            );
}
```

``` text
analyze(...)
generateActions(...)
evaluateQuality(...)
optimizeAnalysis(...)
optimizeActions(...)
        ↓
promptWithPolicy(...)
        ↓
ChatClientRequest Context
        ↓
IncidentPolicyAdvisor
```

이 메서드를 공통화한 이유는 단순히 코드 몇 줄을 줄이기 위해서가
아닙니다.

**LLM을 호출할 때 정책 Context를 전달하는 방식을 하나의 경계로 모으기
위해서입니다.**

기존 JSON 검증, DTO 변환, 예외 처리 코드는 이번 수업에서 제네릭 메서드
등으로 추가 리팩터링하지 않습니다. 이번 수업의 핵심은 코드 중복 제거가
아니라 공통 정책 컨텍스트의 일관된 적용이기 때문입니다.

------------------------------------------------------------------------

## Evaluator와 Optimizer에도 정책 전달하기

수업 14에서는 품질 통제 과정의 LLM 호출에도 동일한
`EscalationDecision`을 전달합니다.

``` text
Analysis Generation
→ EscalationDecision

Analysis Evaluator
→ EscalationDecision

Analysis Optimizer
→ EscalationDecision

Analysis Re-Evaluator
→ EscalationDecision

Action Generation
→ EscalationDecision

Action Evaluator
→ EscalationDecision

Action Optimizer
→ EscalationDecision

Action Re-Evaluator
→ EscalationDecision
```

각 호출은 최종적으로 동일한 정책 전달 경계를 사용합니다.

``` text
각 LLM 호출
    ↓
ChatClientRequest Context
    ↓
IncidentPolicyAdvisor
    ↓
동일한 Common Policy Context
```

------------------------------------------------------------------------

## 서비스 계층의 EscalationDecision 전달

Advisor가 공통 정책을 적용하더라도 `EscalationDecision`은 LLM 호출
지점까지 도달해야 합니다.

``` text
IncidentResponseWorkflow
        ↓
EscalationDecision
        ↓
Quality Control Service
        ↓
Evaluator / Optimizer
        ↓
IncidentChatGateway
        ↓
ChatClientRequest Context
        ↓
IncidentPolicyAdvisor
```

중간 서비스는 정책을 새로 판단하지 않습니다.

``` text
Service Layer
→ 정책 결정 X
→ 정책 변경 X
→ 이미 결정된 정책 전달 O
```

현재는 요청별 정책 데이터의 흐름을 코드에서 명확하게 확인할 수 있도록
`EscalationDecision`을 명시적인 메서드 인자로 전달합니다.

------------------------------------------------------------------------

## 왜 IncidentAiContext 같은 Wrapper를 추가하지 않았을까?

`EscalationDecision`이 여러 메서드를 통과하기 때문에 별도의 요청 Context
객체로 묶는 설계도 가능합니다.

하지만 현재는 `Incident`와 `EscalationDecision` 정도의 제한된 데이터만
함께 이동하므로 Wrapper를 추가하면 오히려 새로운 타입과 추상화 설명이
늘어납니다.

``` text
현재
Incident
+
EscalationDecision
→ 명시적 파라미터 전달

향후 공통 Context가 더 늘어난다면
→ 별도의 요청 Context 객체 검토
```

수업 14에서는 **필요한 통제 문제를 해결하는 범위까지만 추상화를
추가합니다.**

------------------------------------------------------------------------

## 일반적인 AI Guidance까지 모두 Advisor로 옮기지 않는 이유

`EscalationDecision`과 일반적인 AI 행동 지시는 성격이 다릅니다.

``` text
EscalationDecision
→ Java가 결정한 구조화된 정책 데이터

AI Guidance
→ LLM이 응답할 때 따라야 하는 일반 행동 지시
```

이번 수업에서 확인한 문제는 **`EscalationDecision`으로 이미 결정된 정책
컨텍스트가 일부 LLM 호출에는 전달되고 일부 호출에는 전달되지 않는다는
것**입니다.

따라서 모든 Prompt의 공통 문장을 한꺼번에 Advisor로 이동하지 않습니다.
동일한 AI 행동 규칙이 여러 Prompt에서 반복되고 별도의 공통화 필요성이
생긴다면 그때 Common Guidance 구조를 검토할 수 있습니다.

> **Advisor를 배웠기 때문에 이것저것 Advisor로 옮기는 것이 아니라,
> 실제로 확인한 통제 문제를 해결하는 범위까지만 Advisor를 사용합니다.**

------------------------------------------------------------------------

## Advisor 적용 전과 후

### Before --- 수업 13

``` text
Analysis Generation
→ Policy O

Action Generation
→ Policy O

Quality Evaluator
→ Policy X

Quality Optimizer
→ Policy X
```

정책 자체는 구조화되어 있지만 LLM 호출별 정책 전달 책임이 일관되지
않습니다.

### After --- 수업 14

``` text
EscalationDecision
        ↓
각 LLM 호출까지 전달
        ↓
ChatClientRequest Context
        ↓
IncidentPolicyAdvisor
        ↓
Common Policy Context
        ↓
Analysis Generation
Action Generation
Quality Evaluation
Quality Optimization
Re-Evaluation
```

------------------------------------------------------------------------

## IncidentResponseWorkflow에서의 연결

STANDARD / CRITICAL 경로에서는 수업 13과 동일하게 현재 Incident에 적용할
정책을 먼저 결정합니다.

``` text
Incident
    ↓
Escalation Policy Resolution
    ↓
EscalationDecision
```

수업 14에서 중요한 변화는 정책을 사용하는 LLM 호출의 범위입니다.

``` text
EscalationDecision
    ├─→ Analysis Generation
    ├─→ Analysis Evaluator
    ├─→ Analysis Optimizer
    ├─→ Analysis Re-Evaluator
    ├─→ Action Generation
    ├─→ Action Evaluator
    ├─→ Action Optimizer
    └─→ Action Re-Evaluator
```

정책을 결정하는 위치는 변경하지 않고, **정책 컨텍스트를 적용하는 호출
경계만 일관되게 만듭니다.**

------------------------------------------------------------------------

## QUICK 경로

QUICK 경로는 전체 Analysis / Action / Evaluator--Optimizer LLM 흐름을
사용하지 않습니다.

``` text
QUICK
→ 전체 Analysis / Action LLM 흐름 없음
→ Evaluator–Optimizer 없음
```

따라서 수업 14의 공통 정책 Advisor 실험의 핵심 대상은 아닙니다. 다만
기존 QUICK 경로가 정상 동작하는지는 회귀 관점에서 확인할 수 있습니다.

------------------------------------------------------------------------

## 공통 정책 Advisor 실험

수업 14에서는 단위 테스트와 Postman을 이용해 서로 다른 수준의 동작을
확인합니다.

### 실험 1. 정책 Context가 있을 때 Prompt에 공통 정책 추가

`IncidentPolicyAdvisorTest`에서 `ChatClientRequest Context`에
`EscalationDecision`을 넣습니다.

``` text
Original Prompt
        +
Common Policy Context
```

단위 테스트에서는 다음 두 가지를 함께 검증합니다.

``` text
기존 Prompt가 유지되는가?
        +
공통 정책 Context가 추가되는가?
```

즉, `augmentUserMessage(...)` 사용 과정에서 기존 Prompt가 사라지지
않는지도 확인합니다.

### 실험 2. 정책 Context가 없을 때 원래 Request 유지

``` text
ChatClientRequest Context
→ EscalationDecision 없음
        ↓
IncidentPolicyAdvisor
        ↓
Prompt 변경 없음
        ↓
원래 ChatClientRequest 반환
```

단위 테스트에서는 동일한 Request 인스턴스가 반환되고 원래 User Prompt가
유지되는지 확인합니다.

### 실험 3. Postman으로 전체 Incident 처리 흐름 확인

``` json
{
  "sourceSystem": "payment-service",
  "title": "Payment processing failures increased",
  "summary": "Payment requests are intermittently failing and response latency has increased.",
  "severity": "MEDIUM",
  "symptoms": [
    "payment request failures increased",
    "database connection wait time increased",
    "payment response latency increased"
  ]
}
```

핵심 확인:

``` text
requestedSeverity = MEDIUM
effective severity = HIGH

그리고

최종 IncidentResponse 정상 반환
```

Postman 실험은 `IncidentPolicyAdvisor`가 각 Prompt에 어떤 문자열을
추가했는지를 직접 증명하는 테스트가 아닙니다. 그 동작은 Advisor 단위
테스트에서 검증합니다.

Postman에서는 **Advisor와 정책 Context 전달 구조를 추가한 이후에도 전체
Incident 처리 흐름이 정상적으로 동작하는지 확인**합니다.

------------------------------------------------------------------------

## 실험 결과를 해석할 때 주의할 점

### 1. Advisor 단위 테스트와 Postman의 검증 목적은 다릅니다.

``` text
IncidentPolicyAdvisorTest
→ 공통 정책 Context 적용 동작 검증

Postman
→ 전체 Incident 처리 흐름 회귀 확인
```

### 2. 정책 Context 전달은 정책 판단이 아닙니다.

``` text
EscalationDecision
→ Java에서 이미 결정

IncidentPolicyAdvisor
→ 전달 / 표현
```

### 3. Prompt에 정책을 넣는 것과 최종 정책 적용은 다릅니다.

``` text
Policy Context in Prompt
≠
Final Risk Policy
```

최종 Action 위험 통제는 Java 애플리케이션의 책임입니다.

### 4. 명시적 파라미터 전달은 현재 단계의 의도적인 선택입니다.

여러 서비스 메서드에 `EscalationDecision`이 추가되지만, 현재는 요청별
정책 데이터의 흐름을 코드에서 명확하게 확인할 수 있습니다. 공통
Context가 더 많아질 때 별도의 Context 객체를 검토합니다.

------------------------------------------------------------------------

## 핵심 코드 위치

수업 14에서 새로 추가하거나 중요하게 변경되는 주요 클래스는 다음과
같습니다.

  -----------------------------------------------------------------------------
  클래스                                    책임
  ----------------------------------------- -----------------------------------
  `IncidentPolicyAdvisor`                   `EscalationDecision`을 공통 정책
                                            Context로 변환해 LLM 호출 Request에
                                            적용

  `IncidentChatGateway`                     Advisor 등록 및 모든 관련 LLM
                                            호출에 요청별 정책 Context 전달

  `IncidentAnalysisService`                 Analysis 생성 경로에
                                            `EscalationDecision` 전달

  `IncidentActionRecommendationService`     Action 생성 경로에
                                            `EscalationDecision` 전달

  `IncidentAnalysisQualityControlService`   Analysis Evaluator / Optimizer /
                                            Re-Evaluator에 정책 전달

  `IncidentActionQualityControlService`     Action Evaluator / Optimizer /
                                            Re-Evaluator에 정책 전달

  `IncidentQualityEvaluator`                품질 평가 LLM 호출까지 정책 전달

  `IncidentQualityOptimizer`                품질 개선 LLM 호출까지 정책 전달

  `IncidentResponseWorkflow`                한 번 결정된 `EscalationDecision`을
                                            전체 관련 LLM 흐름에 전달

  `IncidentPromptFactory`                   호출별 Prompt 책임 유지, 공통
                                            Escalation Policy 직접 결합 제거

  `IncidentPolicyAdvisorTest`               정책 Context 존재/부재에 따른
                                            Advisor 동작 검증

  `IncidentQualityControlServiceTest`       정책 파라미터가 추가된 품질 통제
                                            흐름의 회귀 검증
  -----------------------------------------------------------------------------

수업 13에서 구현한 severity 정책, `EscalationDecision` 결정, Action 위험
분류, 의미 중복 제거, 최종 외부 응답 정책은 그대로 유지합니다.

------------------------------------------------------------------------

## 정상 흐름 확인

애플리케이션과 테스트를 통해 다음 항목을 순서대로 확인합니다.

1.  애플리케이션이 정상 기동되는지 확인합니다.
2.  `IncidentPolicyAdvisor`가 Spring Bean으로 등록되는지 확인합니다.
3.  `IncidentChatGateway`의 `ChatClient`에 `IncidentPolicyAdvisor`가
    기본 Advisor로 등록되는지 확인합니다.
4.  `EscalationDecision` 결정 로직이 수업 13과 동일하게 유지되는지
    확인합니다.
5.  Analysis / Action Prompt Factory에서 공통 정책 직접 결합 책임이
    제거되었는지 확인합니다.
6.  Analysis 생성과 Evaluator / Optimizer / Re-Evaluator 호출에
    `EscalationDecision`이 전달되는지 확인합니다.
7.  Action 생성과 Evaluator / Optimizer / Re-Evaluator 호출에
    `EscalationDecision`이 전달되는지 확인합니다.
8.  `promptWithPolicy()`가 요청별 정책을 `ChatClientRequest Context`에
    넣는지 확인합니다.
9.  `IncidentPolicyAdvisor.before(...)`가 Context에서 정책을 읽는지
    확인합니다.
10. 기존 User Prompt가 유지되는지 확인합니다.
11. 기존 User Prompt 뒤에 공통 정책 Context가 추가되는지 확인합니다.
12. 정책 Context가 없는 경우 원래 Request가 그대로 반환되는지
    확인합니다.
13. 기존 JSON-only 검증과 DTO 변환이 그대로 동작하는지 확인합니다.
14. 기존 Evaluator--Optimizer 품질 통제가 그대로 동작하는지 확인합니다.
15. 기존 Action 위험 통제가 그대로 동작하는지 확인합니다.
16. `IncidentPolicyAdvisorTest`가 정상적으로 통과하는지 확인합니다.
17. 정책 파라미터가 추가된 기존 품질 통제 단위 테스트가 정상적으로
    통과하는지 확인합니다.
18. Postman으로 실제 Incident 요청을 전송했을 때 최종 응답이 정상적으로
    반환되는지 확인합니다.

------------------------------------------------------------------------

## 수업 14에서 확인하는 범위

이번 수업에서는 다음 내용을 다룹니다.

``` text
- Spring AI Advisor
- BaseAdvisor
- IncidentPolicyAdvisor
- ChatClientRequest
- ChatClientRequest Context
- AdvisorSpec
- advisorSpec.param(...)
- defaultAdvisors(...)
- before(...)
- after(...)
- request.context()
- request.mutate()
- Prompt.getUserMessage()
- Prompt.augmentUserMessage(...)
- 기존 User Prompt 보존
- Common Incident Policy Context
- promptWithPolicy()
- EscalationDecision의 요청별 전달
- Analysis / Action LLM 정책 Context
- Evaluator / Optimizer / Re-Evaluator 정책 Context
- Prompt Factory와 Advisor 책임 분리
- 정책 판단과 정책 Context 전달 책임 분리
- 정책 Context 전달과 최종 정책 적용 책임 분리
- IncidentPolicyAdvisor 단위 테스트
- Postman을 이용한 전체 처리 흐름 확인
```

이번 수업에서는 다음 내용을 다루지 않습니다.

``` text
- Advisor에서 effectiveSeverity 계산
- Advisor에서 새로운 Escalation Policy 결정
- Advisor에서 Action riskLevel 분류
- Advisor에서 위험 Action 포함 / 제외
- Advisor에서 추천 중복 제거
- 모든 Prompt의 일반 AI Guidance를 한꺼번에 Advisor로 이동
- 별도의 IncidentAiContext Wrapper 도입
- ThreadLocal 또는 전역 Context를 이용한 암묵적 정책 전달
- 실제 시스템 변경 Tool
- restart / rollback / scale 실제 실행
- Tool 실행 권한
- 관리자 승인 Workflow
- 승인 대기
- 승인 결과 수신 후 Workflow 재개
- 실제 자동 복구
```

이번 수업의 핵심 범위는 **새로운 정책을 만드는 것**이 아니라 **이미 Java
애플리케이션이 결정한 공통 정책 Context를 여러 LLM 호출에 일관되게
전달하는 것**입니다.

------------------------------------------------------------------------

## 다른 AI 애플리케이션에서 공통 정책 Context를 설계할 때 확인할 것

수업 14의 `IncidentPolicyAdvisor` 구현을 다른 AI 애플리케이션에 그대로
복사하는 것이 목적은 아닙니다.

### 1. 정책 판단과 정책 전달을 구분합니다.

``` text
Policy Decision
    ↓
Structured Policy
    ↓
Policy Delivery
```

Advisor를 사용한다는 이유로 도메인 정책 판단까지 Advisor로 이동하지
않습니다.

### 2. 정말 여러 LLM 호출에 공통으로 필요한 정보인지 확인합니다.

``` text
Call-specific Context
→ Prompt Factory

Common Policy Context
→ Common LLM Call Boundary
```

하나의 Prompt에서만 필요한 정보라면 반드시 Advisor로 이동할 필요는
없습니다.

### 3. 요청별 데이터와 공통 Advisor 동작을 구분합니다.

``` text
Default Advisor
→ 공통 동작

Request Context
→ 현재 요청의 정책 데이터
```

공통 Advisor Bean 내부에 요청별 상태를 저장하지 않습니다.

### 4. 원래 Prompt가 보존되는지 확인합니다.

``` text
Original Prompt
        +
Common Context
        ↓
Final Prompt
```

### 5. 정책이 없을 때의 동작을 정의합니다.

``` text
Policy Exists
→ Context 적용

Policy Missing
→ 원래 Request 유지
```

### 6. Prompt 전달과 최종 정책 적용을 구분합니다.

``` text
Policy Context
    ↓
LLM Response
    ↓
Application Policy
    ↓
External Response
```

LLM이 정책 Context를 받았다는 사실만으로 최종 정책 준수를 보장한다고
가정하지 않습니다.

------------------------------------------------------------------------

## 다음 수업과의 연결

``` text
수업 9
→ 어떤 AI 처리 경로를 실행할 것인가?
→ Flow Control

수업 10
→ LLM에 얼마나 많은 Context를 전달할 것인가?
→ Cost Control

수업 11
→ LLM 출력이 기본 계약을 충족하는가?
→ Quality Control

수업 12
→ 기본 계약을 통과한 출력의 내용도 적절한가?
→ Quality Control

수업 13
→ 위험과 관련된 입력과 AI 추천을
  그대로 최종 판단으로 사용해도 되는가?
→ Risk Control

수업 14
→ 공통 정책 Context를 여러 LLM 호출에
  어떻게 일관되게 전달할 것인가?
→ Risk Control / Policy Delivery

수업 15
→ 복합적인 위험 검토 책임을
  여러 Worker에게 어떻게 나눌 것인가?
→ Risk Control / Responsibility Separation
```

수업 14에서는 이미 결정된 공통 정책 Context를 여러 LLM 호출에 일관되게
전달하도록 구조를 변경했습니다.

``` text
EscalationDecision
        ↓
ChatClientRequest Context
        ↓
IncidentPolicyAdvisor
        ↓
Analysis / Action / Quality LLM Calls
```

하지만 위험 검토가 여러 관점으로 복잡해진다면 다음 문제가 생깁니다.

> **하나의 흐름이 모든 위험 검토를 담당해야 할까요?**

다음 수업에서는 기존 Analysis / Action 생성과 Evaluator--Optimizer
구조를 유지하면서, **복합적인 위험 검토 책임을 여러 Worker에게 나누고 그
결과를 조정하는 구조**를 살펴봅니다.

기술 패턴 자체가 목적은 아닙니다. 위험 검토 책임이 복잡해질 때 역할을
어떻게 분리하고 통제할 것인지가 핵심입니다.

------------------------------------------------------------------------

## 마무리

수업 13에서는 위험과 관련된 입력과 AI 추천에 Java 애플리케이션의 정책을
적용하고, 에스컬레이션 정책 결정 결과를 `EscalationDecision`으로
구조화했습니다.

수업 14에서는 그 다음 문제를 다룹니다.

``` text
정책은 이미 결정되어 있다.
        ↓
하지만 LLM 호출은 여러 개다.
        ↓
각 Prompt가 정책을 직접 구성하면
중복 / 누락 / 불일치가 생길 수 있다.
        ↓
LLM 호출 경계에서
공통 정책 Context를 적용한다.
```

핵심 구조는 다음과 같습니다.

``` text
1. 정책 판단

Java Application
    ↓
EscalationDecision


2. 정책 Context 전달

EscalationDecision
    ↓
ChatClientRequest Context
    ↓
IncidentPolicyAdvisor
    ↓
Common Policy Context


3. 여러 LLM 호출

Analysis
Action Recommendation
Quality Evaluation
Quality Optimization
Re-Evaluation


4. 최종 정책 적용

LLM Output
    ↓
Java Application Policy
    ↓
External Response
```

수업 14에서 가져가야 할 최종 메시지는 다음과 같습니다.

> **정책을 결정하는 것과 여러 LLM 호출에 그 정책 Context를 일관되게
> 전달하는 것은 서로 다른 문제입니다.**

> **정책 판단은 Java 애플리케이션이 수행하고, Advisor는 이미 결정된 정책
> Context를 LLM 호출 경계에 일관되게 적용합니다.**

> **Context는 Advisor의 저장 공간이 아니라, Advisor가 사용할 수 있는
> 요청별 데이터 공간입니다.**

> **Prompt에 정책을 전달하는 것은 정책을 최종 적용하는 것과 다릅니다.
> 최종 정책 적용은 여전히 Java 애플리케이션의 책임입니다.**
