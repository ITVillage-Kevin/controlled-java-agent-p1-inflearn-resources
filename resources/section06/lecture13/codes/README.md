# lecture13-risk-control

수업 13에서 사용하는 코드는 수업 12까지의 Incident Responder에 **외부 심각도 보정, 구조화된 에스컬레이션 정책, AI 추천 위험 분류와 애플리케이션 정책을 추가한 코드**입니다.

수업 12에서는 LLM이 생성한 Analysis와 Action에 대해 기본 계약을 검증하고, Evaluator–Optimizer를 이용해 의미적 품질을 평가하고 필요한 경우 개선했습니다.

하지만 의미적 품질을 통과한 추천이라고 해서 그대로 외부에 전달해도 안전한 것은 아닙니다.

예를 들어 다음과 같은 문제가 남아 있습니다.

* 외부 클라이언트가 중요한 서비스의 장애 심각도를 너무 낮게 요청하는 경우
* YAML에 구조적으로 정의된 에스컬레이션 정책을 단순 문자열로만 사용하는 경우
* 기술적으로 적절한 추천이지만 실제 시스템 상태를 변경할 수 있는 경우
* 표현은 다르지만 의미상 같은 위험 작업이 여러 번 추천되는 경우
* 데이터 삭제와 같이 외부에 그대로 전달하기 부적절한 추천이 생성되는 경우

수업 13에서는 위험과 관련된 입력과 AI 출력을 그대로 최종 판단으로 사용하지 않고 Java 애플리케이션의 정책을 적용합니다.

```text
외부 severity
    ↓
minimumSeverity 정책
    ↓
effectiveSeverity

YAML Policy
    ↓
EscalationDecision
    ↓
Java Application / Prompt / External Response

Quality-Controlled Actions
    ↓
Risk Classification
    ↓
Duplicate Removal
    ↓
Application Policy
    ↓
Final Actions 0~3
```

이 코드 스냅샷의 목적은 LLM이 위험을 완벽하게 판단하도록 만드는 것이 아닙니다.

**위험과 관련된 입력과 AI 출력을 Java 애플리케이션이 어떻게 받아들이고, 어떤 정책을 적용하며, 최종적으로 무엇을 외부에 전달할지 통제하는 것**이 목적입니다.

---

## 이 코드 스냅샷에서 달라진 것

수업 12와 비교해, 수업 13에서는 기존 품질 통제 구조를 유지하면서 그 전후에 리스크 통제 지점을 추가합니다.

| 항목 | 수업 12 | 수업 13 변경 |
| --- | --- | --- |
| Routing | QUICK / STANDARD / CRITICAL | 기존 구조 유지 |
| Context Budget | LLM 입력 Context 크기 제한 | 기존 구조 유지 |
| 기본 계약 검증 | JSON-only + DTO 기본 계약 | 기존 구조 유지 |
| 의미적 품질 통제 | Evaluator–Optimizer | 기존 구조 유지 |
| 외부 severity | 요청값 중심 | `minimumSeverity` 정책으로 보정 |
| 심각도 표현 | 하나의 처리 severity | `requestedSeverity` / effective `severity` 분리 |
| 에스컬레이션 정책 | Prompt에서 사용할 정책 정보 | `EscalationDecision`으로 구조화 |
| Action 위험도 | 별도 위험 분류 없음 | `ActionRiskLevel` 추가 |
| 위험 평가 결과 | 없음 | `ActionRiskAssessment` 추가 |
| 위험 추천 중복 | 별도 의미 중복 통제 없음 | `deduplicationKey` 기반 제거 |
| 외부 추천 정책 | 품질 통과 결과 사용 | Java 정책에 따라 추천 포함/제외 |
| 최종 추천 개수 | 품질 통제까지 정확히 3개 | 위험 통제 후 0~3개 |
| 외부 응답 | 품질 통제 결과 중심 | 위험 정보와 정책 적용 결과 포함 |

수업 13은 기존 Incident Responder를 다시 만드는 것이 아닙니다.

**수업 12까지 확보한 흐름·비용·품질 통제 구조 위에 위험과 관련된 입력과 AI 추천을 다루는 정책 경계를 추가합니다.**

---

## 수업 13 학습 포인트

수업 13에서는 다음 내용을 확인합니다.

* 외부 요청의 `severity`를 그대로 최종 처리 기준으로 사용하지 않는 이유
* `requestedSeverity`와 effective `severity`를 분리하는 이유
* 서비스별 `minimumSeverity` 정책을 적용하는 방법
* `businessCriticality`와 Incident severity의 차이
* YAML 정책을 `EscalationDecision`으로 구조화하는 이유
* 구조화된 정책을 Java 애플리케이션 내부에서 데이터로 유지하는 방법
* LLM과 만나는 Prompt 경계에서 정책을 텍스트로 표현하는 방법
* Incident severity와 Action risk를 별도의 판단으로 구분하는 이유
* `ActionRiskAssessment`로 위험 판단 결과를 구조화하는 방법
* 결정론적인 `ActionRiskClassifier`를 이용해 추천 위험을 분류하는 방법
* 의미상 같은 위험 추천을 `deduplicationKey`로 제거하는 방법
* 애플리케이션 정책에 따라 위험 추천을 외부 응답에서 제외하는 방법
* 위험 통제 후 priority를 다시 정규화하는 방법
* LLM의 3개 Action 출력 계약과 최종 응답의 0~3개 정책을 구분하는 방법
* 모든 추천이 제외되어 최종 추천 목록이 비어 있을 수 있는 이유
* 실제 Tool 실행과 위험 추천 표시를 구분하는 방법

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section05/
    lecture13/
      codes/
        README.md
        lecture13-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

`lecture13-incident-responder/` 디렉터리가 실제 Spring Boot 애플리케이션 프로젝트 루트입니다.

---

## 실행 환경

* Java 21
* Spring Boot 3.5.13
* Spring AI 1.1.4
* Gradle
* OpenAI API Key
* 수업 8에서 구성한 Langfuse 연결 환경

### 실행 전 준비

애플리케이션 실행에는 OpenAI API Key가 필요합니다.

```bash
export OPENAI_API_KEY=your-api-key
```

Windows 환경에서는 IntelliJ IDEA의 Run Configuration에서 환경 변수를 설정할 수 있습니다.

Langfuse 연결 환경을 유지하는 경우 수업 8에서 사용한 환경 변수도 설정합니다.

```text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

실제 API Key와 Authorization 값은 소스코드, `application.yml`, Git 저장소 등에 직접 기록하지 않습니다.

---

## 실행 방법

### 1. 터미널에서 실행

`lecture13-incident-responder/` 프로젝트 루트에서 다음 명령을 실행합니다.

```bash
./gradlew bootRun
```

Windows에서는 다음 명령을 사용할 수 있습니다.

```powershell
.\gradlew.bat bootRun
```

### 2. IntelliJ IDEA에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

애플리케이션 실행 후 콘솔에서 OpenAI API 연결 오류와 Spring Bean 생성 오류가 없는지 확인합니다.

---

## 기본 포트와 API

기본 실행 포트는 `18080`입니다.

```http
POST http://localhost:18080/api/incidents/respond
```

### 정상 요청 예시

`payment-service`의 `minimumSeverity`가 `HIGH`인 정책을 확인하기 위해 다음과 같이 `MEDIUM` 요청을 사용할 수 있습니다.

```json
{
  "sourceSystem": "payment-service",
  "title": "Payment processing latency increased",
  "summary": "Payment requests are taking longer than usual to complete.",
  "severity": "MEDIUM",
  "symptoms": [
    "payment processing latency increased",
    "slow payment response"
  ]
}
```

핵심적으로 확인할 값은 다음과 같습니다.

```text
Request
severity = MEDIUM
    ↓
minimumSeverity = HIGH
    ↓
requestedSeverity = MEDIUM
effective severity = HIGH
    ↓
HIGH 기준 EscalationDecision
```

실제 Analysis와 Action 내용은 LLM의 확률적 동작에 따라 달라질 수 있습니다.

---

## 수업 13에서 해결하려는 문제

### 1. 외부에서 전달한 severity가 애플리케이션의 최종 판단은 아닙니다.

외부 클라이언트가 다음과 같이 요청했다고 가정합니다.

```text
sourceSystem = payment-service
severity = MEDIUM
```

하지만 애플리케이션 정책에 다음 조건이 있을 수 있습니다.

```text
payment-service
minimumSeverity = HIGH
```

외부 요청값을 그대로 처리 기준으로 사용하면 중요한 서비스의 Incident가 정책보다 낮은 심각도로 처리될 수 있습니다.

수업 13에서는 요청값과 정책 적용 결과를 분리합니다.

```text
requestedSeverity
→ 외부 클라이언트가 요청한 값

effectiveSeverity
→ 애플리케이션 정책을 적용한 실제 처리 기준
```

`Incident`에서는 외부 요청값을 `requestedSeverity`로 보존하고, 실제 처리 심각도를 `severity`에 저장합니다.

원본 요청을 덮어쓰는 것이 아니라 **입력값과 정책 적용 결과를 함께 유지**합니다.

---

### 2. 구조화된 정책을 너무 일찍 문자열로 만들면 활용하기 어렵습니다.

에스컬레이션 정책에는 다음과 같은 값이 포함될 수 있습니다.

```text
businessCriticality
severity
acknowledgeWithinMinutes
notifyChannels
approvalRequired
instruction
```

이 값을 애플리케이션 초기에 하나의 문자열로 평탄화하면 Java 코드에서 각각의 정책 값을 명시적으로 다루기 어려워집니다.

수업 13에서는 정책 결정 결과를 `EscalationDecision`으로 유지합니다.

```text
YAML Policy
    ↓
LocalAlertPolicyAdapter
    ↓
EscalationDecision
    ↓
Java Application
```

LLM에 전달해야 하는 시점에만 Prompt에서 읽을 수 있는 텍스트로 표현합니다.

```text
EscalationDecision
    ↓
Prompt Factory
    ↓
LLM-friendly Text
```

**정책 데이터와 정책의 텍스트 표현을 구분하는 것**이 핵심입니다.

---

### 3. 의미 품질이 좋은 추천도 위험할 수 있습니다.

수업 12의 Evaluator가 다음 추천을 현재 Incident에 적절하다고 평가했다고 가정합니다.

```text
Restart the payment-service instances.
```

이 추천은 다음 조건을 만족할 수 있습니다.

```text
Incident와 관련 있음
Analysis와 일치함
구체적임
근거가 설명됨
```

하지만 서비스 재시작은 실제 시스템 상태를 변경할 수 있는 작업입니다.

따라서 다음 두 판단은 서로 다릅니다.

```text
Quality PASS
    ≠
Risk Controlled
```

수업 13에서는 품질 통제를 통과한 Action 뒤에 별도의 위험 통제를 추가합니다.

---

## requestedSeverity와 effectiveSeverity

수업 13에서는 외부 요청 심각도와 애플리케이션의 실제 처리 심각도를 구분합니다.

```text
IncidentRequest.severity
    ↓
requestedSeverity
    ↓
IncidentSeverityPolicy
    +
minimumSeverity
    ↓
effectiveSeverity
```

예를 들어 다음과 같습니다.

```text
requestedSeverity = MEDIUM
minimumSeverity   = HIGH

effectiveSeverity = HIGH
```

요청값이 이미 정책의 최소값보다 높다면 요청값을 낮추지 않습니다.

```text
requestedSeverity = CRITICAL
minimumSeverity   = HIGH

effectiveSeverity = CRITICAL
```

이 정책의 목적은 Routing을 변경하는 것 자체가 아닙니다.

핵심 질문은 다음과 같습니다.

> **외부 클라이언트가 전달한 severity를 애플리케이션이 그대로 신뢰해도 되는가?**

애플리케이션이 실제 처리 심각도를 결정하고, Routing과 에스컬레이션 정책 선택을 포함한 후속 흐름이 그 결과를 사용합니다.

---

## minimumSeverity 정책

서비스별 최소 처리 심각도는 정책으로 관리합니다.

개념적인 흐름은 다음과 같습니다.

```text
sourceSystem
    ↓
Service Policy
    ↓
minimumSeverity
```

이 값은 외부에서 요청한 severity와 비교되어 effective severity를 결정합니다.

```text
requestedSeverity
        +
minimumSeverity
        ↓
IncidentSeverityPolicy
        ↓
effectiveSeverity
```

`minimumSeverity`는 외부 요청을 무시하기 위한 값이 아닙니다.

외부 요청값은 `requestedSeverity`로 보존하면서, 애플리케이션이 보장해야 하는 최소 처리 기준을 별도로 적용합니다.

---

## businessCriticality와 Incident severity

`businessCriticality`와 Incident severity는 같은 개념이 아닙니다.

```text
businessCriticality
→ 서비스 자체의 비즈니스 중요도

Incident severity
→ 현재 Incident의 처리 심각도
```

예를 들어 `payment-service`가 비즈니스상 매우 중요한 서비스라고 해서 모든 Incident의 severity가 항상 `CRITICAL`이어야 하는 것은 아닙니다.

반대로 현재 Incident의 severity가 높다고 해서 서비스 자체의 business criticality가 변경되는 것도 아닙니다.

두 값은 정책에서 서로 다른 의미를 갖습니다.

---

## EscalationDecision

`EscalationDecision`은 현재 Incident에 적용할 에스컬레이션 정책의 결정 결과를 구조화해서 전달합니다.

개념적으로 다음과 같은 정보를 포함합니다.

```text
EscalationDecision

service
businessCriticality
severity
acknowledgeWithinMinutes
notifyChannels
approvalRequired
instruction
```

이 객체의 목적은 정책을 결정하는 로직 자체를 담는 것이 아닙니다.

**이미 결정된 정책 결과를 구조화된 데이터로 다음 단계에 전달하는 것**이 목적입니다.

```text
Policy Definition
    ↓
Policy Resolution
    ↓
EscalationDecision
    ↓
Analysis / Action / External Response
```

---

## Prompt 경계에서 정책 표현하기

Java 애플리케이션 내부에서는 에스컬레이션 정책을 `EscalationDecision`으로 유지합니다.

LLM에게 전달할 때는 결국 텍스트 형태의 Prompt가 필요합니다.

수업 13에서는 이 변환을 Prompt를 구성하는 경계에서 수행합니다.

```text
Policy Adapter
    ↓
EscalationDecision
    ↓
Java Application
    ↓
Prompt Factory
    ↓
LLM-friendly Text
```

이 구조를 통해 정책의 원본 표현과 LLM용 표현을 구분할 수 있습니다.

수업 13에서는 Analysis와 Action 생성 Prompt에 필요한 정책 문맥을 전달합니다.

여러 LLM 호출에 공통 정책 문맥을 일관되게 적용하는 문제는 다음 수업에서 별도로 다룹니다.

---

## ActionRiskLevel

`ActionRiskLevel`은 AI가 생성한 개별 추천 Action의 위험 수준을 표현합니다.

수업 13의 강의용 분류에서는 다음과 같은 수준을 사용합니다.

```text
LOW
MEDIUM
HIGH
CRITICAL
```

예를 들면 다음과 같습니다.

```text
Read-only investigation
→ LOW

Unknown change
→ MEDIUM

Restart / Rollback
→ HIGH

Destructive change
→ CRITICAL
```

이 값은 Incident의 severity와 별개의 판단입니다.

```text
Incident Severity
→ 현재 장애의 처리 심각도

Action Risk Level
→ 개별 추천 작업 자체의 위험도
```

따라서 다음과 같은 추론을 해서는 안 됩니다.

```text
effectiveSeverity가 HIGH
    ↓
모든 Action riskLevel도 HIGH
```

Incident severity와 Action risk는 서로 다른 통제 축입니다.

---

## ActionRiskAssessment

`ActionRiskAssessment`는 한 추천에 대한 위험 판단 결과를 구조화해서 전달합니다.

개념적으로 다음과 같은 정보를 포함합니다.

```text
ActionRiskAssessment
    ↓
riskLevel
approvalRequired
riskReason
deduplicationKey
allowed
```

`ActionRiskAssessment` 자체가 위험을 판단하는 것은 아닙니다.

위험 분류 결과를 다음 단계에 전달하는 데이터 타입입니다.

```text
Recommended Action
    ↓
ActionRiskClassifier
    ↓
ActionRiskAssessment
```

최종 외부 응답에는 필요한 위험 정보가 `RecommendedActionItem`에 포함됩니다.

---

## approvalRequired의 의미

`approvalRequired=true`는 Incident Responder가 실제 승인 절차를 실행한다는 의미가 아닙니다.

수업 13의 범위는 다음과 같습니다.

```text
AI Recommendation
    ↓
Risk Classification
    ↓
approvalRequired 표시
    ↓
riskReason 설명
    ↓
안전한 형태로 외부 전달
```

다음 과정은 포함하지 않습니다.

```text
승인 요청
승인 대기
관리자 승인
승인 결과 수신
Workflow 재개
실제 Tool 실행
자동 복구
```

`approvalRequired`는 추천의 위험 특성과 외부 검토 필요성을 표현하는 정보입니다.

---

## ActionRiskClassifier

`ActionRiskClassifier`는 AI가 생성한 추천을 Java 애플리케이션의 제한된 규칙으로 분류합니다.

수업 13에서는 위험 통제 흐름을 명확하게 보여주기 위해 키워드 기반의 결정론적인 규칙으로 단순화합니다.

개념적인 분류는 다음과 같습니다.

```text
Recommended Action
    ↓
ActionRiskClassifier
    ↓
destructive?
rollback?
restart?
read-only?
unknown?
    ↓
ActionRiskAssessment
```

예를 들어 다음과 같은 추천은 서로 다르게 처리될 수 있습니다.

```text
Check database connection pool metrics.
→ LOW

Restart payment-service pods.
→ HIGH

Rollback the latest deployment.
→ HIGH

Delete database records.
→ CRITICAL
```

키워드 기반 분류는 운영 환경에서 사용할 완전한 위험 분류 모델이 아닙니다.

실제 운영 환경에서는 다음과 같은 검증된 Context를 함께 사용할 수 있습니다.

```text
구조화된 action type
대상 서비스
환경
변경 범위
권한
승인 정책
감사 정보
```

이번 강의에서는 같은 입력에 같은 결과를 얻을 수 있는 결정론적인 규칙을 사용해 **위험 통제 지점 자체**에 집중합니다.

---

## deduplicationKey와 의미 중복 제거

문자열이 다르더라도 의미상 같은 위험 작업이 여러 번 추천될 수 있습니다.

예를 들어 다음 두 문장은 표현이 다릅니다.

```text
Restart payment-service pods.

Restart the payment-service instances.
```

하지만 두 추천 모두 같은 종류의 서비스 재시작을 의미할 수 있습니다.

수업 13에서는 분류 결과에 의미 키를 포함할 수 있습니다.

```text
Restart payment-service pods
        ┐
        ├─ service-restart
        │
Restart payment-service instances
        ┘
```

`IncidentRiskPolicyService`는 이 키를 이용해 의미상 같은 위험 추천을 중복 제거합니다.

문자열이 다르다는 이유만으로 서로 다른 운영 변경으로 취급하지 않습니다.

---

## IncidentRiskPolicyService

`IncidentRiskPolicyService`는 품질 통제를 마친 Action에 애플리케이션의 위험 정책을 적용합니다.

전체 흐름은 다음과 같습니다.

```text
Quality-Controlled Actions
        ↓
Risk Classification
        ↓
Semantic Duplicate Removal
        ↓
Application Policy
        ↓
Priority Normalization
        ↓
Final Actions 0~3
```

주요 책임은 다음과 같습니다.

* 각 Action을 `ActionRiskClassifier`로 분류합니다.
* 분류 결과를 `ActionRiskAssessment`로 받습니다.
* `deduplicationKey`를 기준으로 의미상 같은 추천을 제거합니다.
* 애플리케이션 정책상 외부 응답에 포함하지 않을 추천을 제외합니다.
* 남은 추천의 상대적인 순서를 유지합니다.
* 남은 추천의 priority를 1부터 다시 부여합니다.
* 모든 추천이 제외되는 경우 빈 추천 목록을 허용합니다.

핵심은 다음과 같습니다.

> **LLM이 생성한 결과를 반드시 모두 외부에 반환해야 하는 것은 아닙니다.**

최종적으로 어떤 추천을 외부에 전달할지는 Java 애플리케이션이 결정합니다.

---

## LLM 출력 계약과 최종 응답 정책

수업 11에서 정의한 Action 기본 계약은 수업 13에서도 유지됩니다.

```text
LLM Output Contract
→ exactly 3 actions
```

수업 12의 의미 품질 통제 이후에도 이 계약을 유지합니다.

```text
Quality-Controlled Output
→ exactly 3 actions
```

하지만 수업 13의 위험 정책을 적용한 이후 최종 외부 응답은 달라질 수 있습니다.

```text
Risk-Controlled Final Response
→ 0~3 actions
```

예를 들어 3개의 Action 중 하나가 중복 제거되면 다음과 같습니다.

```text
Quality-Controlled Actions
→ 3

Risk Control
→ duplicate 1 removed

Final Actions
→ 2
```

모든 추천이 정책에 의해 제외될 수도 있습니다.

```text
Quality-Controlled Actions
→ 3

Risk Control
→ all excluded

Final Actions
→ 0
```

이 경우 3개를 맞추기 위해 LLM을 다시 호출하지 않습니다.

**LLM의 기본 출력 계약과 외부 응답 정책은 서로 다른 통제 경계입니다.**

---

## 위험 통제 전과 후

### Before — 수업 12

```text
Incident
    ↓
Analysis Generation
    ↓
Basic Validation
    ↓
Analysis Quality Control
    ↓
Action Generation
    ↓
Basic Validation
    ↓
Action Quality Control
    ↓
Incident Response
```

의미 품질을 평가하고 개선할 수 있지만, 품질을 통과한 추천의 운영 위험을 별도로 통제하지 않습니다.

### After — 수업 13

```text
Incident Request
    ↓
requestedSeverity
    ↓
minimumSeverity Policy
    ↓
effectiveSeverity
    ↓
EscalationDecision
    ↓
Analysis / Action
    ↓
Basic + Semantic Quality Control
    ↓
ActionRiskClassifier
    ↓
IncidentRiskPolicyService
    ↓
Risk-Controlled Actions
    ↓
Incident Response
```

수업 13에서는 위험과 관련된 입력과 AI 출력을 Java 애플리케이션 정책 안에서 처리합니다.

---

## IncidentResponseWorkflow에서의 연결

STANDARD / CRITICAL 경로의 핵심 흐름에서는 현재 Incident에 적용할 에스컬레이션 정책을 결정합니다.

```text
Incident
    ↓
Runbook / History
    ↓
Escalation Policy Resolution
    ↓
EscalationDecision
```

이 정책은 Analysis와 Action 생성에 전달됩니다.

```text
EscalationDecision
    ├─→ Analysis
    └─→ Action Recommendation
```

Action은 기존 품질 통제를 거친 뒤 위험 정책으로 전달됩니다.

```text
Action LLM
    ↓
Basic Contract
    ↓
Evaluator–Optimizer
    ↓
Quality-Controlled Actions
    ↓
IncidentRiskPolicyService
    ↓
Risk-Controlled Actions
```

최종 `IncidentResponse`에는 요청 심각도와 실제 처리 심각도, 구조화된 정책, 위험 통제된 추천을 포함할 수 있습니다.

---

## QUICK 경로

QUICK 경로는 `IncidentResponseWorkflow`의 전체 Analysis / Action LLM 흐름을 사용하지 않습니다.

따라서 수업 13의 AI 추천 위험 통제 핵심 실험 대상도 아닙니다.

```text
QUICK
→ LLM Action Recommendation 없음
→ IncidentRiskPolicyService 적용 대상 없음
```

다만 공통 `IncidentResponse`와 severity 관련 구조가 변경되었으므로 기존 QUICK 경로가 정상 동작하는지는 회귀 관점에서 확인할 가치가 있습니다.

---

## 위험 통제 실험

수업 13에서는 Postman과 단위 테스트를 이용해 서로 다른 통제 지점을 확인합니다.

### 실험 1. requestedSeverity → effectiveSeverity

`payment-service`의 최소 심각도가 `HIGH`라고 가정합니다.

요청:

```json
{
  "sourceSystem": "payment-service",
  "title": "Payment processing latency increased",
  "summary": "Payment requests are taking longer than usual to complete.",
  "severity": "MEDIUM",
  "symptoms": [
    "payment processing latency increased",
    "slow payment response"
  ]
}
```

확인할 흐름:

```text
Request severity
→ MEDIUM

minimumSeverity
→ HIGH

requestedSeverity
→ MEDIUM

effective severity
→ HIGH

EscalationDecision
→ HIGH 기준 정책
```

이 실험은 외부 요청값과 애플리케이션의 정책 적용 결과를 구분해서 확인하는 것이 목적입니다.

`effectiveSeverity`가 높아졌다고 해서 Action의 `riskLevel`까지 자동으로 높아지는 것은 아닙니다.

---

### 실험 2. restart 동의어 중복 제거

테스트 입력:

```text
1. Restart payment-service pods
2. Restart the payment-service instances
3. Check database connection pool metrics
```

앞의 두 추천은 문장이 다르지만 같은 서비스 재시작으로 분류될 수 있습니다.

```text
Restart payment-service pods
        ┐
        ├─ service-restart
        │       ↓
Restart instances
        ┘   one action remains

Check DB pool
→ read-only action remains
```

기대 결과:

```text
Final Actions
→ 2 actions

priority
→ 1, 2

restart riskLevel
→ HIGH

restart approvalRequired
→ true
```

이 테스트는 문자열 일치가 아니라 애플리케이션이 부여한 의미 키를 이용해 위험 작업의 중복을 통제하는지 확인합니다.

---

### 실험 3. 모든 위험 추천 제외

테스트 입력:

```text
1. Delete database records
2. Drop table containing corrupted data
3. Disable audit logging
```

강의용 위험 분류 정책에서는 세 추천 모두 destructive change로 분류될 수 있습니다.

```text
Quality-Controlled Actions
→ 3 actions

Risk Classification
→ CRITICAL
→ CRITICAL
→ CRITICAL

Application Policy
→ exclude
→ exclude
→ exclude

Final recommendedActions
→ []
```

이 결과는 수업 11의 "Action 정확히 3개" 계약이 깨졌다는 의미가 아닙니다.

LLM과 품질 통제 단계에서는 3개의 유효한 Action을 확보했습니다.

그 이후 수업 13의 Java 애플리케이션 정책이 외부에 전달할 추천을 0개로 결정한 것입니다.

제외된 Action을 채우기 위해 추가 LLM 호출을 수행하지 않습니다.

---

## 실험 결과를 해석할 때 주의할 점

### 1. 높은 Incident severity가 높은 Action risk를 의미하지 않습니다.

```text
Incident severity = HIGH
≠
모든 Action riskLevel = HIGH
```

장애 심각도와 추천 작업의 위험도는 서로 다른 판단입니다.

### 2. approvalRequired는 실제 승인 실행이 아닙니다.

```text
approvalRequired = true
→ 외부 검토가 필요한 위험 추천이라는 정보

approvalRequired = true
≠ 승인 Workflow 실행
```

### 3. 위험 분류는 강의용 결정론적 단순화입니다.

키워드 기반 `ActionRiskClassifier`는 실제 운영 환경의 완전한 위험 판단 모델이 아닙니다.

수업에서는 위험 통제 지점을 명확하게 보여주기 위한 결정론적인 예제로 사용합니다.

### 4. 위험 통제 후 Action 개수는 줄어들 수 있습니다.

```text
LLM / Quality Contract
→ 3 actions

Risk-Controlled Response
→ 0~3 actions
```

최종 외부 응답 정책은 LLM 출력 계약과 별개의 책임입니다.

---

## 핵심 코드 위치

수업 13에서 새로 추가하거나 중요하게 변경되는 주요 클래스는 다음과 같습니다.

| 클래스 | 책임 |
| --- | --- |
| `Incident` | requested severity와 effective severity 구분 |
| `IncidentResponse` | 요청/처리 심각도, 구조화 정책, 위험 통제 결과 외부 전달 |
| `IncidentSeverityPolicy` | requested severity와 minimum severity를 이용한 실제 처리 심각도 결정 |
| `ServicePolicyDefinition` | 서비스별 정책 정의 |
| `AlertPolicyCatalog` | 서비스 정책 조회 |
| `EscalationDecision` | 현재 Incident에 적용된 에스컬레이션 정책 결과 구조화 |
| `AlertPolicyPort` | 구조화된 에스컬레이션 정책 조회 경계 |
| `LocalAlertPolicyAdapter` | 로컬 정책 설정을 이용한 `EscalationDecision` 생성 |
| `ActionRiskLevel` | 추천 Action 위험 수준 표현 |
| `ActionRiskAssessment` | 개별 Action 위험 판단 결과 구조화 |
| `ActionRiskClassifier` | 결정론적 규칙으로 추천 위험 분류 |
| `RecommendedActionItem` | 외부 응답용 추천과 위험 정보 표현 |
| `RiskControlledActions` | 위험 정책 적용 후 추천 결과 표현 |
| `IncidentRiskPolicyService` | 위험 분류, 의미 중복 제거, 추천 포함/제외, priority 정규화 |
| `IncidentResponderService` | 요청 severity 보존 및 effective severity 적용 |
| `IncidentResponseWorkflow` | 구조화 정책과 위험 통제 흐름 연결 |
| `IncidentPromptFactory` | 수업 13에서는 Analysis/Action Prompt에 정책 문맥 표현 |
| `IncidentAnalysisService` | Analysis 생성에 구조화 정책 전달 |
| `IncidentActionRecommendationService` | Action 생성에 구조화 정책 전달 |

기존 수업에서 구현한 Routing, Context Budget, JSON-only 검증, 기본 출력 계약 검증, Evaluator–Optimizer 및 Observability 구조는 유지합니다.

---

## 정상 흐름 확인

애플리케이션 실행 후 다음 항목을 순서대로 확인합니다.

1. 애플리케이션이 정상 기동되는지 확인합니다.
2. 기존 Incident API가 정상적으로 호출되는지 확인합니다.
3. 외부 요청 severity가 `requestedSeverity`로 보존되는지 확인합니다.
4. 서비스별 `minimumSeverity` 정책이 적용되는지 확인합니다.
5. effective `severity`가 정책에 따라 결정되는지 확인합니다.
6. 이후 Routing이 실제 처리 severity를 사용하는지 확인합니다.
7. `EscalationDecision`이 문자열이 아니라 구조화된 객체로 생성되는지 확인합니다.
8. 에스컬레이션 정책이 effective severity를 기준으로 선택되는지 확인합니다.
9. Analysis와 Action 생성에 구조화된 정책이 전달되는지 확인합니다.
10. 기존 기본 계약 검증이 그대로 동작하는지 확인합니다.
11. 기존 Evaluator–Optimizer 품질 통제가 그대로 동작하는지 확인합니다.
12. 품질 통제를 마친 Action이 `IncidentRiskPolicyService`로 전달되는지 확인합니다.
13. 각 Action이 `ActionRiskClassifier`로 분류되는지 확인합니다.
14. 의미상 같은 위험 추천이 중복 제거되는지 확인합니다.
15. 정책상 허용되지 않는 추천이 최종 외부 응답에서 제외되는지 확인합니다.
16. 남은 Action의 priority가 다시 정규화되는지 확인합니다.
17. 최종 추천이 0~3개가 될 수 있는지 확인합니다.
18. 최종 응답에 requested/effective severity와 구조화된 escalation policy가 포함되는지 확인합니다.
19. 위험 추천에 필요한 `riskLevel`, `approvalRequired`, `riskReason`이 포함되는지 확인합니다.
20. QUICK 경로가 기존 방식대로 정상 동작하는지 확인합니다.
21. 위험 정책 단위 테스트가 정상적으로 통과하는지 확인합니다.

---

## 수업 13에서 확인하는 범위

이번 수업에서는 다음 내용을 다룹니다.

```text
- requestedSeverity / effectiveSeverity 분리
- minimumSeverity 정책
- IncidentSeverityPolicy
- businessCriticality와 Incident severity 구분
- EscalationDecision
- 구조화된 에스컬레이션 정책
- Prompt 경계에서 정책 텍스트 표현
- ActionRiskLevel
- ActionRiskAssessment
- ActionRiskClassifier
- riskLevel
- approvalRequired
- riskReason
- deduplicationKey
- allowed
- 의미상 같은 위험 추천 중복 제거
- IncidentRiskPolicyService
- 위험 추천 외부 응답 포함/제외
- 위험 통제 후 priority 정규화
- LLM Action 3개 계약
- Risk-Controlled Final Response 0~3개
- Postman을 이용한 severity 보정 확인
- 단위 테스트를 이용한 중복 제거와 전체 추천 제외 확인
```

이번 수업에서는 다음 내용을 다루지 않습니다.

```text
- 실제 시스템 변경 Tool
- restart / rollback / scale 실제 실행
- Tool 실행 권한
- 관리자 승인 Workflow
- 승인 대기
- 승인 결과 수신 후 Workflow 재개
- 실제 자동 복구
- 위험 판단 전체를 LLM에 위임하는 구조
- 운영 환경 수준의 완전한 위험 분류 엔진
```

이번 수업의 핵심 범위는 **AI 추천을 실제로 실행하는 것**이 아니라 **위험 추천을 식별하고, 구조화하고, Java 애플리케이션 정책을 적용해 안전한 형태의 외부 응답으로 만드는 것**입니다.

---

## 다른 AI 애플리케이션에서 리스크 통제를 설계할 때 확인할 것

수업 13의 구체적인 키워드 분류 규칙을 다른 AI 애플리케이션에 그대로 적용하는 것이 목적은 아닙니다.

각 애플리케이션의 업무와 위험 수준에 맞는 통제 정책을 설계해야 합니다.

### 1. 외부 입력 중 신뢰하면 안 되는 값을 식별합니다.

```text
External Input
    ↓
그대로 신뢰해도 되는가?
    ↓
Application Policy
    ↓
Effective Value
```

외부 요청값과 애플리케이션의 최종 처리 기준을 구분해야 하는지 검토합니다.

### 2. 정책 결과를 구조화합니다.

```text
Policy Definition
    ↓
Policy Decision
    ↓
Structured Decision
```

정책을 너무 일찍 문자열로 만들지 않고 이후 코드에서 필요한 값을 명시적으로 사용할 수 있도록 유지합니다.

### 3. 업무 품질과 위험도를 구분합니다.

```text
좋은 추천인가?
    ≠
안전한 추천인가?
```

내용이 적절하다는 평가와 실제 업무에서 허용할 수 있다는 판단은 서로 다른 통제 문제일 수 있습니다.

### 4. 위험 분류에 사용할 검증된 Context를 정의합니다.

```text
Action
    +
Target
    +
Environment
    +
Scope
    +
Permission / Policy
    ↓
Risk Assessment
```

운영 환경에서는 단순 텍스트 키워드보다 구조화되고 검증된 정보를 우선적으로 활용하는 것이 좋습니다.

### 5. 최종 외부 응답 정책을 별도로 정의합니다.

```text
AI Output
    ↓
Validation / Quality
    ↓
Risk Policy
    ↓
External Response
```

AI가 생성한 결과를 모두 반환해야 한다고 가정하지 않습니다.

중복 제거, 제외, 위험 정보 표시 등 외부 응답 정책을 애플리케이션이 명시적으로 관리할 수 있습니다.

### 6. AI 출력 계약과 외부 응답 계약을 구분합니다.

```text
AI Output Contract
→ 모델 출력에 요구하는 구조

External Response Policy
→ 실제 사용자에게 전달할 결과
```

두 경계의 개수나 조건이 반드시 같을 필요는 없습니다.

---

## 다음 수업과의 연결

```text
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
→ 이미 결정된 공통 정책 문맥을
  여러 LLM 호출에 어떻게 일관되게 전달할 것인가?
→ Risk Control / Policy Context
```

수업 13에서는 Java 애플리케이션이 위험과 관련된 정책을 결정하고 AI 추천에 위험 정책을 적용합니다.

하지만 Incident Responder에는 이제 하나의 LLM 호출만 존재하지 않습니다.

```text
Analysis Generation
Action Generation
Quality Evaluation
Quality Optimization
Re-Evaluation
```

수업 13에서는 Analysis와 Action 생성 Prompt에 에스컬레이션 정책을 전달하고 있지만, LLM 호출 지점이 늘어날수록 각 Prompt가 공통 정책을 직접 구성하는 방식은 중복과 누락을 만들 수 있습니다.

다음 수업에서는 **이미 Java 애플리케이션이 결정한 정책 자체는 그대로 유지하면서, 공통 정책 문맥을 여러 LLM 호출 경계에 일관되게 적용하는 방법**을 다룹니다.

---

## 마무리

수업 12에서는 LLM 출력의 의미적 품질을 평가하고 필요한 경우 개선했습니다.

하지만 품질 통제를 통과했다고 해서 위험 통제까지 완료된 것은 아닙니다.

수업 13에서는 다음 세 가지 리스크 통제 지점을 추가합니다.

```text
1. 외부 심각도 통제

requestedSeverity
    ↓
minimumSeverity
    ↓
effectiveSeverity


2. 에스컬레이션 정책 구조화

YAML Policy
    ↓
EscalationDecision
    ↓
Prompt / External Response


3. AI 추천 위험 통제

Quality-Controlled Actions
    ↓
Risk Classification
    ↓
Duplicate Removal
    ↓
Application Policy
    ↓
Final Actions 0~3
```

이 구조의 목적은 위험 판단을 모두 LLM에게 맡기는 것이 아닙니다.

Java 애플리케이션이 외부 입력에 정책을 적용하고, 정책 결정 결과를 구조화된 데이터로 유지하며, AI가 생성한 추천의 위험을 분류하고, 최종적으로 무엇을 외부에 전달할지 결정합니다.

수업 13에서 가져가야 할 최종 메시지는 다음과 같습니다.

> **의미 품질을 통과한 AI 출력이라도 위험 통제까지 완료된 것은 아닙니다.**

> **위험과 관련된 입력이나 AI 출력을 그대로 최종 판단으로 사용하지 않습니다.**

> **애플리케이션이 정책을 적용하고, 그 결과를 구조화된 결정으로 다음 단계에 전달합니다.**

> **LLM이 생성한 결과를 모두 외부에 전달할 필요는 없습니다. 최종 외부 응답은 Java 애플리케이션의 정책이 결정합니다.**
