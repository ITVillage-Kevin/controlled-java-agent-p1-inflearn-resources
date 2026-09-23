# lecture12-evaluator-optimizer

수업 12에서 사용하는 코드는 수업 11까지의 Incident Responder에 **LLM 출력의 의미적 품질 평가와 개선을 위한 Evaluator–Optimizer 구조를 추가한 코드**입니다.

수업 11에서는 LLM이 생성한 응답에 대해 JSON 형식 검증, Java 객체 변환, 필수값과 목록 개수 등의 기본 계약 검증, 재시도 및 Fallback 처리를 추가했습니다.

하지만 기본 계약을 통과했다고 해서 LLM이 생성한 내용까지 신뢰할 수 있는 것은 아닙니다.

예를 들어 다음과 같은 문제가 발생할 수 있습니다.

* Incident에서 제공하지 않은 정보를 사실처럼 설명하는 경우
* Runbook이나 과거 장애 이력과 일치하지 않는 원인을 제시하는 경우
* 불확실한 상황에서 특정 원인을 확정적으로 설명하는 경우
* 현재 Incident와 관계없는 일반적인 추천 Action을 생성하는 경우
* Analysis에서 제시한 원인과 Action의 내용이 일치하지 않는 경우

이러한 문제는 필수값이나 목록 개수 등을 검사하는 기본 Validator만으로 판단하기 어렵습니다.

수업 12에서는 LLM이 생성한 결과를 별도의 Evaluator가 평가하고, 품질 개선이 필요하다고 판단하면 Optimizer를 통해 수정합니다.

수정된 결과는 기존 기본 계약 검증을 다시 거친 후 재평가합니다.

```text
LLM Output
    ↓
수업 11 기본 계약 검증
    ↓
Evaluator
    ↓
PASS / NEEDS_REVISION
    ↓
필요하면 Optimizer 1회
    ↓
수정 결과 기본 계약 재검증
    ↓
Re-Evaluator
    ↓
최종 결과 및 품질 상태 결정
```

이 코드 스냅샷의 목적은 LLM이 생성한 모든 결과의 정확성을 보장하는 것이 아닙니다.

**LLM에게 평가와 수정을 맡기더라도, 언제 수정하고 몇 번 수정하며 언제 종료할지는 Java 애플리케이션이 결정하도록 만드는 것**이 목적입니다.

---

## 이 코드 스냅샷에서 달라진 것

수업 11과 비교해, 수업 12에서는 기존 출력 계약 검증 구조를 유지하면서 의미적 품질 평가와 개선 단계를 추가합니다.

| 항목             | 수업 11                        | 수업 12 변경                         |
| -------------- | ---------------------------- | -------------------------------- |
| Routing        | QUICK / STANDARD / CRITICAL  | 기존 구조 유지                         |
| Context Budget | LLM 입력 Context 크기 제한         | 기존 구조 유지                         |
| JSON 형식 검증     | JSON-only 검증                 | 기존 구조 재사용                        |
| DTO 기본 계약      | 필수값, 목록 개수, 중복 및 priority 검증 | 기존 구조 재사용                        |
| 기본 계약 실패 처리    | 재시도 및 Fallback               | 기존 구조 유지                         |
| 의미적 품질 평가      | 별도 평가 단계 없음                  | Evaluator 추가                     |
| 출력 개선          | 기본 계약 위반 시 재시도               | 의미적 품질 개선을 위한 Optimizer 추가       |
| 수정 결과 검증       | 기본 계약 검증                     | Optimizer 결과에 기본 계약 재검증 적용       |
| 재평가            | 별도 의미적 재평가 없음                | 수정된 결과를 Re-Evaluator로 재평가        |
| 수정 횟수          | 기본 계약 재시도 정책                 | 의미적 품질 개선 최대 1회                  |
| 최종 상태          | 기본 계약 통과 여부 중심               | PASSED / IMPROVED / NOT_RESOLVED |
| 관찰             | 기존 Trace 및 Span              | 품질 상태 전환 Observation event 추가    |

수업 12는 기존 애플리케이션을 다시 만드는 것이 아닙니다.

**수업 11에서 확보한 기본 계약 검증 구조 위에 의미적 품질 평가와 개선 단계를 추가합니다.**

---

## 수업 12 학습 포인트

수업 12에서는 다음 내용을 확인합니다.

* 기본 계약 검증과 의미적 품질 평가의 차이
* Evaluator–Optimizer 패턴이 필요한 이유
* Evaluator와 Optimizer의 역할 분리
* Analysis와 Action에 서로 다른 평가 기준을 적용하는 방법
* 평가 결과를 구조화된 객체로 표현하는 방법
* Evaluator의 판단과 애플리케이션의 최종 품질 상태를 구분하는 이유
* 평가 결과에 따라 Java 애플리케이션이 실행 흐름을 결정하는 방법
* Optimizer가 수정한 결과를 기존 Validator로 다시 검증하는 이유
* 수정된 결과를 다시 Evaluator로 평가하는 이유
* 의미적 품질 개선 횟수를 최대 1회로 제한하는 이유
* 품질 문제가 해결되지 않았을 때 NOT_RESOLVED로 종료하는 방법
* 품질 통제 과정의 주요 상태 전환을 Observation event로 기록하는 방법
* IDE Console과 Langfuse를 이용해 실제 품질 개선 흐름을 관찰하는 방법
* 단위 테스트를 통해 최대 수정 횟수와 종료 정책을 검증하는 방법

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section05/
    lecture12/
      codes/
        README.md
        lecture12-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

`lecture12-incident-responder/` 디렉터리가 실제 Spring Boot 애플리케이션 프로젝트 루트입니다.

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

Langfuse에서 요청 흐름을 관찰하려면 수업 8에서 사용한 환경 변수도 유지합니다.

```text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

실제 API Key와 Authorization 값은 소스코드, `application.yml`, Git 저장소 등에 직접 기록하지 않습니다.

---

## 실행 방법

### 1. 터미널에서 실행

`lecture12-incident-responder/` 프로젝트 루트에서 다음 명령을 실행합니다.

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

```json
{
  "sourceSystem": "order-service",
  "title": "Order API HTTP 500 and database timeout increase",
  "summary": "Order API HTTP 500 errors are increasing with database timeout and connection pool saturation.",
  "severity": "HIGH",
  "symptoms": [
    "HTTP 500",
    "database timeout",
    "connection pool saturation"
  ]
}
```

이 요청은 기존 Routing 정책에 따라 STANDARD 분석 경로로 처리됩니다.

```text
Incident Request
    ↓
STANDARD_ANALYSIS
    ↓
Analysis LLM
    ↓
Analysis 기본 계약 검증
    ↓
Analysis 의미적 품질 평가 및 개선
    ↓
Action LLM
    ↓
Action 기본 계약 검증
    ↓
Action 의미적 품질 평가 및 개선
    ↓
Incident Response
```

수업 12에서는 기존 Routing 정책을 변경하지 않습니다.

동일한 Incident 요청을 사용하면서 LLM 출력의 의미적 품질 평가와 개선 과정에 집중합니다.

---

## 수업 12에서 해결하려는 문제

### 1. 기본 계약을 통과해도 내용이 올바르지 않을 수 있습니다.

수업 11에서는 Analysis 결과에 다음과 같은 기본 계약을 적용했습니다.

```text
situationSummary 필수

likelyCauses 최소 개수 충족

impactAssessment 필수

confidenceNote 필수
```

예를 들어 다음과 같은 Analysis 결과가 생성되었다고 가정합니다.

```json
{
  "situationSummary": "Order API errors are increasing.",
  "likelyCauses": [
    "The authentication server has completely failed."
  ],
  "impactAssessment": "All customer accounts are permanently inaccessible.",
  "confidenceNote": "The authentication server is definitely the root cause."
}
```

JSON 형식에는 문제가 없습니다.

필수 필드도 존재하고 문자열도 비어 있지 않습니다.

하지만 실제 Incident에서는 데이터베이스 타임아웃과 Connection Pool 포화 현상만 보고되었습니다.

인증 서버 장애가 발생했다는 근거는 제공되지 않았습니다.

따라서 이 응답은 기본 계약을 충족하더라도 다음과 같은 문제가 있습니다.

```text
기본 계약 검증
    ↓
PASS

의미적 품질 평가
    ↓
근거 없는 원인 제시

제공되지 않은 장애 영향 설명

과도한 확신
```

이러한 문제는 단순히 문자열이 비어 있는지, 목록 개수가 올바른지 검사하는 방식으로 판단하기 어렵습니다.

### 2. Action의 형식이 올바르더라도 추천 내용이 부적절할 수 있습니다.

다음과 같은 Action이 생성되었다고 가정합니다.

```text
Action 1
→ Check the database connection pool.

Action 2
→ Review the authentication server configuration.

Action 3
→ Inspect the frontend CSS files.
```

Action이 정확히 3개이고 priority가 1, 2, 3으로 설정되어 있더라도 모든 추천이 현재 Incident와 관련 있다고 볼 수는 없습니다.

특히 프런트엔드 CSS 파일 검사는 현재 보고된 데이터베이스 타임아웃 문제와 직접적인 관련성이 부족합니다.

수업 12에서는 다음 질문을 추가합니다.

```text
이 추천은 현재 Incident와 관련이 있는가?

Analysis에서 제시한 원인과 일치하는가?

추천 내용이 구체적인가?

추천 이유가 충분한가?

priority가 현재 상황에 적절한가?
```

### 3. 평가 결과만으로 품질이 개선되는 것은 아닙니다.

Evaluator가 다음과 같이 판단했다고 가정합니다.

```text
NEEDS_REVISION
```

이는 현재 결과에 개선이 필요하다는 평가입니다.

하지만 평가 결과만 반환한다고 해서 실제 출력이 개선되는 것은 아닙니다.

따라서 평가 결과를 바탕으로 Optimizer가 출력을 수정하고, 수정된 결과가 실제로 개선되었는지 다시 평가하는 과정이 필요합니다.

---

## 품질 통제 전과 후

### Before — 수업 11

```text
LLM Output
    ↓
JSON-only 검증
    ↓
Java 객체 변환
    ↓
기본 계약 검증
    ↓
PASS
    ↓
최종 결과 사용
```

이 구조에서는 기본 계약을 충족하더라도 내용의 의미적 품질에 문제가 있을 수 있습니다.

### After — 수업 12

```text
LLM Output
    ↓
수업 11 기본 계약 검증
    ↓
Evaluator
    ↓
┌─────────────────────────┐
│ PASS                    │
│ → 최초 결과 채택         │
│ → PASSED                │
└─────────────────────────┘

┌─────────────────────────┐
│ NEEDS_REVISION          │
│ → Optimizer 1회         │
│ → 기본 계약 재검증       │
│ → Re-Evaluator          │
│                         │
│ PASS                    │
│ → IMPROVED              │
│                         │
│ NEEDS_REVISION          │
│ → NOT_RESOLVED          │
└─────────────────────────┘
```

기본 계약 검증과 의미적 품질 평가를 별도의 단계로 구분합니다.

또한 LLM의 평가 결과에 따라 Java 애플리케이션이 다음 실행 경로를 결정합니다.

---

## Evaluator–Optimizer 구조

Evaluator–Optimizer는 LLM이 생성한 결과를 평가하고, 필요한 경우 개선하는 패턴입니다.

수업 12에서는 다음 세 가지 책임을 구분합니다.

| 구성 요소                   | 역할                                       |
| ----------------------- | ---------------------------------------- |
| Evaluator               | 현재 출력의 의미적 품질 평가                         |
| Optimizer               | 평가 결과를 바탕으로 출력 수정                        |
| Quality Control Service | 평가 결과에 따른 분기, 수정 횟수 제한, 결과 채택 및 종료 정책 적용 |

Evaluator와 Optimizer는 모두 LLM을 사용할 수 있습니다.

하지만 전체 품질 통제 흐름을 LLM에게 맡기지는 않습니다.

```text
Evaluator
→ 현재 결과에 대한 품질 판단

Optimizer
→ 개선이 필요한 결과 수정

Java Application
→ 수정 여부 결정
→ 수정 횟수 제한
→ 수정 결과 검증
→ 최종 결과 채택
→ 품질 루프 종료
```

핵심은 **LLM은 평가와 수정을 수행하고, 애플리케이션은 실행 흐름과 종료 조건을 통제한다는 것**입니다.

---

## IncidentQualityEvaluation

`IncidentQualityEvaluation`은 Evaluator가 생성한 의미적 품질 평가 결과를 표현하는 객체입니다.

Evaluator는 단순히 통과 여부만 반환하는 것이 아니라, 현재 출력의 품질을 판단한 결과와 개선에 필요한 피드백을 제공합니다.

```text
IncidentQualityEvaluation
    ↓
현재 출력에 대한 품질 판단
    ↓
품질 문제가 발견된 이유
    ↓
개선에 필요한 피드백
```

이 평가 결과는 이후 Optimizer가 어떤 부분을 수정해야 하는지 판단하는 데 사용됩니다.

또한 Java 애플리케이션이 Optimizer를 호출할지 결정하는 입력이 됩니다.

Evaluator의 판단이 곧바로 최종 응답의 상태가 되는 것은 아닙니다.

---

## IncidentQualityDecision

`IncidentQualityDecision`은 Evaluator가 현재 출력에 대해 내린 판단을 표현합니다.

수업 12에서는 다음 두 가지 판단을 사용합니다.

| 판단               | 의미                         |
| ---------------- | -------------------------- |
| `PASS`           | 현재 출력이 평가 기준을 충족한다고 판단     |
| `NEEDS_REVISION` | 현재 출력에 의미적 품질 개선이 필요하다고 판단 |

예를 들어 Evaluator가 `PASS`를 반환하면 Java 애플리케이션은 추가적인 Optimizer 호출 없이 최초 결과를 채택할 수 있습니다.

반면 `NEEDS_REVISION`이면 정해진 정책에 따라 Optimizer를 호출합니다.

```text
Evaluator
    ↓
PASS
    ↓
추가 수정 없이 종료
```

```text
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer 호출
```

이 판단은 현재 출력에 대한 의미적 품질 평가입니다.

전체 품질 통제 과정이 어떤 상태로 종료되었는지를 표현하는 값과는 구분해야 합니다.

---

## QualityControlStatus

`QualityControlStatus`는 Java 애플리케이션이 품질 통제 과정을 완료한 후 관리하는 최종 상태입니다.

수업 12에서는 다음 세 가지 상태를 사용합니다.

| 상태             | 의미                                   |
| -------------- | ------------------------------------ |
| `PASSED`       | 최초 출력이 의미적 품질 평가를 통과                 |
| `IMPROVED`     | Optimizer가 수정한 결과가 기본 계약 검증과 재평가를 통과 |
| `NOT_RESOLVED` | 정해진 수정 정책 안에서 품질 문제가 해결되지 않음         |

Evaluator의 판단과 애플리케이션의 최종 상태는 서로 다릅니다.

```text
LLM Evaluator

PASS
NEEDS_REVISION

→ 현재 출력에 대한 품질 판단
```

```text
Java Application

PASSED
IMPROVED
NOT_RESOLVED

→ 전체 품질 통제 과정의 최종 상태
```

예를 들어 최초 Evaluator가 `NEEDS_REVISION`을 반환하더라도 Optimizer의 수정 결과가 재평가를 통과하면 최종 상태는 `IMPROVED`가 됩니다.

반대로 수정 결과가 재평가에서도 `NEEDS_REVISION`이면 최종 상태는 `NOT_RESOLVED`가 됩니다.

**Evaluator의 판단은 품질 통제 과정의 입력이고, 최종 상태는 Java 애플리케이션이 정한 정책에 따라 결정됩니다.**

---

## QualityControlledResult

`QualityControlledResult`는 품질 통제 과정이 완료된 후의 결과를 표현합니다.

이 객체는 품질 통제된 출력과 최종 품질 상태를 함께 전달하는 역할을 합니다.

```text
QualityControlledResult
    ↓
최종 출력
    +
QualityControlStatus
```

단순히 수정된 출력 객체만 반환하면 해당 결과가 최초 평가를 통과한 것인지, Optimizer를 통해 개선된 것인지, 품질 문제가 해결되지 않은 상태인지 구분하기 어렵습니다.

따라서 최종 출력과 품질 통제 상태를 함께 관리합니다.

```text
최초 평가 통과
    ↓
QualityControlledResult
    ↓
PASSED
```

```text
수정 후 재평가 통과
    ↓
QualityControlledResult
    ↓
IMPROVED
```

```text
수정 후에도 품질 문제 미해결
    ↓
QualityControlledResult
    ↓
NOT_RESOLVED
```

이 구조를 통해 호출자는 최종 출력뿐 아니라 품질 통제 과정이 어떤 상태로 종료되었는지도 확인할 수 있습니다.

---

## IncidentQualityEvaluator

`IncidentQualityEvaluator`는 LLM을 이용하여 출력의 의미적 품질을 평가하는 컴포넌트입니다.

수업 12에서는 Analysis와 Action에 서로 다른 평가 기준을 적용합니다.

### Analysis 평가 기준

Analysis에서는 다음과 같은 항목을 평가합니다.

| 평가 항목  | 확인할 내용                           |
| ------ | -------------------------------- |
| 근거 일치성 | Incident와 제공된 Context에 근거한 설명인가? |
| 원인 분석  | 제시한 원인이 관찰된 증상과 관련이 있는가?         |
| 구체성    | 현재 Incident의 특징을 반영한 분석인가?       |
| 확신 수준  | 불확실한 내용을 확정적으로 설명하지 않는가?         |
| 장애 영향  | 제공된 정보에 비추어 영향 범위를 적절하게 설명하는가?   |

예를 들어 데이터베이스 타임아웃이 보고된 Incident에서 인증 서버 장애를 확정적인 원인으로 제시했다면 근거 일치성에 문제가 있을 수 있습니다.

Evaluator는 이러한 문제를 확인하고 개선이 필요한 경우 `NEEDS_REVISION`을 반환합니다.

### Action 평가 기준

Action에서는 다음과 같은 항목을 평가합니다.

| 평가 항목              | 확인할 내용                    |
| ------------------ | ------------------------- |
| Incident relevance | 추천이 현재 Incident와 관련이 있는가? |
| Analysis alignment | 추천이 Analysis의 내용과 일치하는가?  |
| Action specificity | 추천이 현재 상황에 맞게 구체적인가?      |
| Rationale quality  | 추천 이유가 충분하고 적절한가?         |
| Priority quality   | 추천 순서가 현재 상황에 적절한가?       |

Action 평가에는 현재 Incident뿐 아니라 앞 단계에서 생성한 Analysis도 필요합니다.

```text
Incident
    +
Analysis
    +
Recommended Actions
    ↓
Action Evaluator
```

이러한 Context를 함께 전달해야 추천 Action이 실제 장애 상황과 Analysis에 적절하게 연결되는지 평가할 수 있습니다.

수업 12에서는 Action의 의미적 적절성을 평가합니다.

위험 추천 분류와 정책 적용은 이후 리스크 통제 수업에서 별도로 다룹니다.

---

## IncidentQualityOptimizer

`IncidentQualityOptimizer`는 Evaluator의 평가 결과를 바탕으로 LLM 출력을 수정하는 컴포넌트입니다.

Optimizer는 단순히 최초 Prompt를 다시 실행하는 역할이 아닙니다.

현재 출력과 Evaluator의 피드백을 참고하여 문제가 발견된 부분을 개선합니다.

```text
최초 LLM Output
    +
Evaluator Feedback
    ↓
IncidentQualityOptimizer
    ↓
Optimized Output
```

예를 들어 Evaluator가 다음과 같은 문제를 발견했다고 가정합니다.

```text
문제

제공된 근거에 없는 원인을 확정적으로 설명함.

개선 방향

현재 Incident와 Runbook에 근거하여
원인을 다시 설명하고 불확실성을 명시할 것.
```

Optimizer는 이러한 피드백을 바탕으로 출력을 수정합니다.

하지만 Optimizer가 반환한 결과를 그대로 최종 결과로 사용하지는 않습니다.

수정 과정에서 기본 계약을 위반하거나 의미적 품질 문제가 남아 있을 수 있기 때문입니다.

---

## Optimizer 결과에 기본 계약 검증을 다시 적용하는 이유

수업 11에서 기본 계약을 통과한 결과를 Optimizer에 전달하더라도 수정 후에는 다시 검증해야 합니다.

예를 들어 최초 Action 결과가 다음과 같다고 가정합니다.

```text
Action 3개

priority = 1, 2, 3

필수값 정상
```

Optimizer가 내용을 수정하면서 다음과 같은 결과를 생성할 수 있습니다.

```text
Action 2개

priority = 1, 1

필수값 일부 누락
```

의미적 품질을 개선하려는 과정에서 기존의 기본 계약이 깨질 수 있습니다.

따라서 수정된 결과에도 수업 11에서 구현한 Validator를 다시 적용합니다.

```text
Optimizer
    ↓
Optimized Output
    ↓
기존 Basic Validator
    ↓
PASS / FAIL
```

기본 계약을 통과하지 못한 결과는 의미적 품질이 개선되었는지와 관계없이 정상적인 출력으로 채택할 수 없습니다.

**기본 계약 검증은 최초 LLM 출력뿐 아니라 수정된 출력에도 동일하게 적용해야 합니다.**

---

## Re-Evaluator

Re-Evaluator는 Optimizer가 수정한 결과의 의미적 품질을 다시 평가하는 단계입니다.

별도의 새로운 평가 기준을 만드는 것이 아니라, 최초 평가에 사용한 Evaluator를 재사용합니다.

```text
최초 Output
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
기본 계약 재검증
    ↓
Re-Evaluator
```

재평가 결과가 `PASS`이면 수정된 결과를 채택합니다.

재평가에서도 `NEEDS_REVISION`이면 정해진 수정 횟수 안에서 품질 문제가 해결되지 않은 것으로 처리합니다.

```text
Re-Evaluator
    ↓
PASS
    ↓
IMPROVED
```

```text
Re-Evaluator
    ↓
NEEDS_REVISION
    ↓
NOT_RESOLVED
```

재평가 단계에서 중요한 것은 최초 평가와 동일한 품질 기준을 적용하는 것입니다.

평가 대상은 수정된 결과로 변경되지만, 품질을 판단하는 기준 자체가 달라져서는 안 됩니다.

---

## IncidentQualityControlService

`IncidentQualityControlService`는 수업 12의 품질 통제 흐름을 관리하는 핵심 컴포넌트입니다.

Evaluator와 Optimizer가 각각 평가와 수정을 수행한다면, `IncidentQualityControlService`는 평가 결과에 따라 다음 단계를 결정합니다.

```text
IncidentQualityControlService
    ↓
Evaluator 호출
    ↓
평가 결과 확인
    ↓
수정 필요 여부 결정
    ↓
Optimizer 호출
    ↓
수정 결과 검증
    ↓
Re-Evaluator 호출
    ↓
최종 결과 및 상태 결정
```

이 서비스가 담당하는 주요 정책은 다음과 같습니다.

* 최초 평가를 통과하면 추가 수정 없이 종료합니다.
* 개선이 필요한 경우에만 Optimizer를 호출합니다.
* Optimizer 호출 횟수를 최대 1회로 제한합니다.
* 수정 결과에 기존 기본 계약 검증을 다시 적용합니다.
* 기본 계약을 통과한 수정 결과를 재평가합니다.
* 재평가 결과에 따라 최종 출력과 상태를 결정합니다.
* 품질 문제가 해결되지 않아도 추가 Optimizer 호출 없이 종료합니다.

이러한 정책을 Evaluator나 Optimizer 내부에 분산시키지 않고 Java 애플리케이션의 품질 통제 서비스에서 관리합니다.

---

## 품질 개선 횟수를 최대 1회로 제한하는 이유

Evaluator와 Optimizer를 반복 호출하면 출력이 계속 개선될 것이라고 기대할 수 있습니다.

하지만 반복 호출이 항상 품질 개선을 보장하는 것은 아닙니다.

또한 반복 횟수가 증가하면 다음과 같은 문제가 발생할 수 있습니다.

```text
LLM 호출 횟수 증가

Token 사용량 증가

전체 응답 시간 증가

반복 종료 시점 예측 어려움
```

수업 12에서는 이러한 문제를 방지하기 위해 의미적 품질 개선 횟수를 최대 1회로 제한합니다.

```text
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer 1회
    ↓
기본 계약 검증
    ↓
Re-Evaluator
    ↓
최종 종료
```

재평가에서 다시 `NEEDS_REVISION`이 반환되더라도 Optimizer를 추가로 호출하지 않습니다.

이 정책을 통해 품질 개선 시도 횟수에 명확한 상한을 설정합니다.

수업 11의 기본 계약 재시도와 수업 12의 의미적 품질 개선은 서로 다른 처리 단계입니다.

따라서 의미적 품질 개선을 최대 1회로 제한한다고 해서 전체 Incident 처리 과정의 모든 LLM 호출이 1회로 제한되는 것은 아닙니다.

---

## 최종 결과 채택 정책

수업 12에서는 품질 통제 과정이 종료되는 상황을 세 가지로 구분합니다.

### 1. PASSED

최초 출력이 Evaluator의 평가를 통과한 경우입니다.

```text
Initial Output
    ↓
Evaluator
    ↓
PASS
    ↓
PASSED
    ↓
Initial Output 채택
```

이 경우 Optimizer를 호출하지 않습니다.

### 2. IMPROVED

최초 출력에 개선이 필요했지만 Optimizer가 수정한 결과가 기본 계약 검증과 재평가를 통과한 경우입니다.

```text
Initial Output
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
Optimized Output
    ↓
Basic Validator PASS
    ↓
Re-Evaluator PASS
    ↓
IMPROVED
    ↓
Optimized Output 채택
```

최종 결과로 수정된 출력을 사용합니다.

### 3. NOT_RESOLVED

정해진 수정 정책 안에서 품질 문제가 해결되지 않은 경우입니다.

```text
Initial Output
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
Optimized Output
    ↓
Re-Evaluator
    ↓
NEEDS_REVISION
    ↓
NOT_RESOLVED
```

이 경우 Optimizer를 추가로 호출하지 않습니다.

수업 12의 품질 통제 정책에서는 품질 문제가 해결되지 않은 수정 결과를 최종 출력으로 채택하지 않고 최초 출력을 유지합니다.

다만 최초 출력을 유지한다는 것이 해당 출력의 의미적 품질이 충분하다는 뜻은 아닙니다.

`NOT_RESOLVED`는 정해진 수정 정책 안에서 품질 문제가 해결되지 않았음을 나타냅니다.

**품질 문제가 해결되지 않았을 때 이를 성공으로 처리하지 않고 명시적인 상태로 구분하는 것도 품질 통제의 일부입니다.**

---

## Analysis 품질 통제 흐름

Analysis에서는 Incident와 제공된 Context를 바탕으로 생성한 분석 결과의 의미적 품질을 평가합니다.

```text
Incident Request
    ↓
Analysis LLM
    ↓
수업 11 기본 계약 검증
    ↓
IncidentQualityControlService
    ↓
Analysis Evaluator
    ↓
PASS / NEEDS_REVISION
    ↓
필요하면 Analysis Optimizer
    ↓
Analysis 기본 계약 재검증
    ↓
Analysis Re-Evaluator
    ↓
QualityControlledResult
    ↓
최종 Analysis
```

Analysis Evaluator는 다음과 같은 문제를 확인합니다.

```text
근거 없는 원인 제시

Incident와 관련 없는 분석

불충분한 설명

과도한 확신

제공된 Context와 일치하지 않는 판단
```

품질 개선이 필요한 경우 Optimizer는 Evaluator의 피드백을 바탕으로 Analysis를 수정합니다.

수정된 Analysis는 기본 계약 검증과 재평가를 거쳐 최종 결과 채택 여부가 결정됩니다.

---

## Action 품질 통제 흐름

Action에서는 Incident와 Analysis를 함께 사용하여 추천 내용의 의미적 품질을 평가합니다.

```text
Incident Request
    +
Final Analysis
    ↓
Action LLM
    ↓
수업 11 기본 계약 검증
    ↓
IncidentQualityControlService
    ↓
Action Evaluator
    ↓
PASS / NEEDS_REVISION
    ↓
필요하면 Action Optimizer
    ↓
Action 기본 계약 재검증
    ↓
Action Re-Evaluator
    ↓
QualityControlledResult
    ↓
최종 Action
```

Action의 의미적 품질은 Action 자체만으로 판단하기 어렵습니다.

현재 Incident와 Analysis를 함께 확인해야 추천이 실제 장애 상황에 적절한지 판단할 수 있습니다.

예를 들어 Analysis에서 데이터베이스 Connection Pool 포화를 주요 원인 후보로 제시했는데 Action에서는 프런트엔드 CSS 파일 검사를 우선 추천했다면 두 결과 사이의 관련성을 검토해야 합니다.

수업 12에서는 이러한 의미적 품질 문제를 평가하고 필요한 경우 수정합니다.

---

## Observation event로 품질 통제 흐름 관찰하기

수업 12에서는 품질 통제 과정의 주요 상태 전환을 Observation event로 기록합니다.

기존 수업에서는 Trace와 Span을 통해 요청 흐름과 LLM 호출을 관찰했습니다.

이번 수업에서는 해당 흐름 안에서 애플리케이션이 어떤 품질 통제 판단을 내렸는지도 확인합니다.

```text
Incident Request
    ↓
Analysis Quality Control
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
Re-Evaluator
    ↓
IMPROVED
```

이러한 상태 전환을 Observation event로 기록하면 IDE Console에서 품질 통제 과정의 주요 판단을 확인할 수 있습니다.

### IDE Console에서 확인할 내용

```text
최초 평가 결과

Optimizer 호출 여부

수정 결과의 기본 계약 검증 결과

재평가 결과

최종 품질 상태
```

Observation event는 LLM의 응답 내용과 별도로 애플리케이션이 어떤 결정을 내렸는지 관찰하는 데 사용합니다.

---

## Langfuse에서 품질 개선 흐름 관찰하기

Langfuse에서는 실제 LLM 호출 흐름을 확인합니다.

수업 12에서는 Evaluator와 Optimizer가 추가되므로 기존 Analysis 및 Action 생성 외에도 품질 평가와 개선을 위한 LLM 호출이 발생할 수 있습니다.

```text
Incident Request
    ↓
Analysis Generation
    ↓
Analysis Evaluation
    ↓
필요하면 Analysis Optimization
    ↓
Analysis Re-Evaluation
    ↓
Action Generation
    ↓
Action Evaluation
    ↓
필요하면 Action Optimization
    ↓
Action Re-Evaluation
```

Langfuse에서 다음 항목을 확인합니다.

| 항목          | 확인할 내용                       |
| ----------- | ---------------------------- |
| Trace       | Incident 요청 전체 흐름            |
| Span Tree   | 서비스 및 LLM 호출의 실행 구조          |
| Generation  | 실제 LLM 호출과 응답                |
| Prompt      | Evaluator와 Optimizer에 전달된 입력 |
| Output      | 평가 및 수정 결과                   |
| Token Usage | 품질 통제 과정에서 사용한 Token         |
| Latency     | 각 호출 및 전체 요청의 처리 소요 시간       |

IDE Console과 Langfuse는 서로 다른 관찰 목적을 가집니다.

```text
IDE Console
→ Java 애플리케이션의 품질 통제 판단 확인
```

```text
Langfuse
→ 실제 LLM 호출과 Prompt, Output,
  Token Usage 및 Latency 확인
```

수업 12에서는 두 관찰 결과를 함께 사용하여 품질 개선 과정이 실제로 어떻게 실행되었는지 확인합니다.

---

## 품질 통제 실험

수업 12에서는 실제 LLM 호출과 단위 테스트를 통해 품질 통제 흐름을 검증합니다.

### 실험 1. 실제 LLM을 이용한 품질 개선

의미적 품질 문제가 있는 출력을 유도한 후 Evaluator와 Optimizer가 실제로 동작하는지 확인합니다.

목표로 하는 실행 흐름은 다음과 같습니다.

```text
Initial Output
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
Optimized Output
    ↓
Basic Validator PASS
    ↓
Re-Evaluator PASS
    ↓
IMPROVED
```

이 실험에서는 다음 내용을 확인합니다.

1. 최초 출력에 대해 Evaluator가 품질 평가를 수행하는지 확인합니다.
2. `NEEDS_REVISION`이면 Optimizer가 호출되는지 확인합니다.
3. Optimizer가 평가 피드백을 바탕으로 출력을 수정하는지 확인합니다.
4. 수정된 결과가 기본 계약 검증을 통과하는지 확인합니다.
5. Re-Evaluator가 수정된 결과를 다시 평가하는지 확인합니다.
6. 재평가 결과가 `PASS`이면 최종 상태가 `IMPROVED`가 되는지 확인합니다.

실제 LLM은 확률적으로 동작하므로 동일한 Prompt에서도 매번 동일한 평가와 수정 결과가 생성된다고 보장할 수 없습니다.

최초 평가에서 `PASS`가 반환되면 Optimizer를 호출하지 않고 `PASSED`로 종료하는 것이 정상입니다.

재평가에서 `NEEDS_REVISION`이 반환되면 `NOT_RESOLVED`로 종료하는 것이 정상입니다.

---

### 실험 2. 단위 테스트로 최대 수정 횟수 확인

실제 LLM 호출만으로는 모든 실패 경로를 안정적으로 재현하기 어렵습니다.

따라서 수업 12에서는 `IncidentQualityControlService`의 단위 테스트를 통해 품질 문제가 해결되지 않는 상황을 검증합니다.

테스트에서 재현하는 흐름은 다음과 같습니다.

```text
Initial Output
    ↓
Evaluator
    ↓
NEEDS_REVISION
    ↓
Optimizer
    ↓
Optimized Output
    ↓
Basic Validator PASS
    ↓
Re-Evaluator
    ↓
NEEDS_REVISION
    ↓
NOT_RESOLVED
    ↓
추가 Optimizer 호출 없음
```

이 테스트에서는 다음 사항을 확인합니다.

* 최초 평가에서 개선이 필요하다고 판단하면 Optimizer를 호출하는지 확인합니다.
* 수정된 결과에 대해 재평가를 수행하는지 확인합니다.
* 재평가에서도 개선이 필요하다고 판단하면 `NOT_RESOLVED`로 종료하는지 확인합니다.
* Optimizer가 최대 1회만 호출되는지 확인합니다.
* 품질 문제가 해결되지 않았을 때 최초 출력을 유지하는지 확인합니다.

이 단위 테스트의 목적은 LLM이 생성한 내용의 정확성을 검증하는 것이 아닙니다.

**LLM이 어떤 평가 결과를 반환하더라도 Java 애플리케이션이 정해진 수정 횟수와 종료 정책을 지키는지 검증하는 것**입니다.

---

## 실험 결과를 해석할 때 주의할 점

수업 12에서 사용하는 Evaluator 역시 LLM입니다.

따라서 Evaluator가 `PASS`를 반환했다고 해서 해당 출력의 사실 정확성이나 업무상 적절성이 완전히 보장되는 것은 아닙니다.

마찬가지로 Optimizer가 출력을 수정했다고 해서 반드시 품질이 개선되었다고 단정할 수 없습니다.

```text
Evaluator PASS
≠ 사실 정확성 보장
```

```text
Optimizer 실행
≠ 품질 개선 보장
```

수업 12에서는 이러한 한계를 인정하고 다음과 같은 통제 구조를 적용합니다.

```text
Evaluator 판단
    ↓
Java 애플리케이션의 정책 적용
    ↓
필요하면 Optimizer
    ↓
기본 계약 재검증
    ↓
Re-Evaluator
    ↓
명시적인 최종 상태
```

이 구조는 LLM의 판단 자체를 결정론적으로 만드는 것이 아닙니다.

LLM의 평가와 수정 결과를 받아들이는 애플리케이션의 처리 방식을 명시적으로 통제하는 구조입니다.

---

## 핵심 코드 위치

수업 12에서 새로 추가하거나 중요하게 변경되는 주요 클래스는 다음과 같습니다.

| 클래스                                   | 책임                                        |
| ------------------------------------- | ----------------------------------------- |
| `IncidentQualityEvaluation`           | Evaluator의 의미적 품질 평가 결과 표현                |
| `IncidentQualityDecision`             | PASS / NEEDS_REVISION 판단 표현               |
| `QualityControlStatus`                | PASSED / IMPROVED / NOT_RESOLVED 최종 상태 표현 |
| `QualityControlledResult`             | 최종 출력과 품질 통제 상태 전달                        |
| `IncidentQualityEvaluator`            | Analysis 및 Action의 의미적 품질 평가              |
| `IncidentQualityOptimizer`            | 평가 피드백을 바탕으로 출력 수정                        |
| `IncidentQualityControlService`       | 평가, 수정, 재검증, 재평가 및 종료 정책 관리               |
| `IncidentAiOutputContract`            | 품질 평가 결과의 출력 변환 계약 추가                     |
| `IncidentChatGateway`                 | 평가 및 수정에 필요한 LLM 호출                       |
| `IncidentPromptFactory`               | Evaluator 및 Optimizer Prompt 구성           |
| `IncidentAnalysisService`             | Analysis 생성 이후 의미적 품질 통제 연결               |
| `IncidentActionRecommendationService` | Action 생성 이후 의미적 품질 통제 연결                 |

기존 수업에서 구현한 JSON-only 검증, 기본 출력 계약 검증, Routing, Context Budget 및 Observability 구조는 유지합니다.

---

## 정상 흐름 확인

애플리케이션 실행 후 다음 항목을 순서대로 확인합니다.

1. 애플리케이션이 정상 기동되는지 확인합니다.
2. 기존 Incident API가 정상적으로 호출되는지 확인합니다.
3. STANDARD 요청이 기존 Routing 정책에 따라 처리되는지 확인합니다.
4. Analysis LLM이 정상적으로 호출되는지 확인합니다.
5. Analysis 결과가 수업 11의 기본 계약 검증을 통과하는지 확인합니다.
6. Analysis Evaluator가 의미적 품질 평가를 수행하는지 확인합니다.
7. 평가 결과가 `PASS`이면 Optimizer 없이 종료하는지 확인합니다.
8. 평가 결과가 `NEEDS_REVISION`이면 Optimizer를 호출하는지 확인합니다.
9. 수정된 Analysis에 기본 계약 검증을 다시 적용하는지 확인합니다.
10. 수정된 Analysis를 Re-Evaluator가 다시 평가하는지 확인합니다.
11. 평가 결과에 따라 최종 Analysis와 품질 상태를 결정하는지 확인합니다.
12. Action 생성 이후에도 동일한 품질 통제 흐름이 적용되는지 확인합니다.
13. Action Evaluator가 Incident와 Analysis를 함께 참고하는지 확인합니다.
14. 품질 개선 횟수가 최대 1회로 제한되는지 확인합니다.
15. 품질 문제가 해결되지 않으면 `NOT_RESOLVED`로 종료하는지 확인합니다.
16. IDE Console에서 품질 통제 상태 전환을 확인합니다.
17. Langfuse에서 실제 LLM 호출과 Prompt, Output, Token Usage 및 Latency를 확인합니다.
18. 단위 테스트를 통해 최대 수정 횟수와 종료 정책을 확인합니다.

---

## 수업 12에서 확인하는 범위

이번 수업에서는 다음 내용을 다룹니다.

```text
- 기본 계약 검증과 의미적 품질 평가의 차이
- Evaluator–Optimizer 패턴
- Analysis 의미적 품질 평가
- Action 의미적 품질 평가
- IncidentQualityEvaluation
- IncidentQualityDecision
- QualityControlStatus
- QualityControlledResult
- IncidentQualityEvaluator
- IncidentQualityOptimizer
- IncidentQualityControlService
- Evaluator의 PASS / NEEDS_REVISION 판단
- Java 애플리케이션의 PASSED / IMPROVED / NOT_RESOLVED 상태
- Evaluator 결과에 따른 실행 흐름 분기
- Optimizer 최대 1회 호출 정책
- 수정 결과의 기본 계약 재검증
- 수정 결과의 의미적 품질 재평가
- 최종 결과 채택 정책
- 품질 상태 전환 Observation event
- IDE Console을 이용한 품질 통제 흐름 확인
- Langfuse를 이용한 LLM 호출 흐름 확인
- IncidentQualityControlService 단위 테스트
```

이번 수업에서는 다음 내용을 다루지 않습니다.

```text
- LLM 출력의 사실 정확성에 대한 완전한 보장
- 모든 품질 문제의 자동 해결
- 무제한 Evaluator–Optimizer 반복
- 위험 추천의 riskLevel 분류
- approvalRequired / riskReason
- minimumSeverity 및 escalation policy
- 실제 Tool 실행
- 관리자 승인
- 실제 시스템 변경
- 자동 복구
```

이번 수업의 핵심 범위는 의미적 품질 평가와 개선 흐름입니다.

위험 추천 분류와 정책 적용은 이후 리스크 통제 수업에서 별도로 다룹니다.

---

## 다른 AI 애플리케이션에서 Evaluator–Optimizer를 설계할 때 확인할 것

수업 12의 구조를 다른 AI 애플리케이션에 그대로 적용하는 것이 목적은 아닙니다.

각 애플리케이션의 업무 요구사항과 비용, 응답 시간 및 품질 기준에 따라 필요한 통제 정책을 설계해야 합니다.

### 1. 평가 기준을 먼저 정의합니다.

```text
현재 출력
    ↓
무엇을 평가할 것인가?
    ↓
어떤 조건에서 개선이 필요한가?
```

평가 기준이 명확하지 않으면 Evaluator의 판단을 애플리케이션의 실행 정책과 연결하기 어렵습니다.

### 2. 평가와 수정을 분리합니다.

```text
Evaluator
→ 현재 결과의 문제 판단

Optimizer
→ 평가 결과를 바탕으로 수정
```

평가와 수정을 별도의 책임으로 구분하면 각 단계의 입력과 출력을 확인하기 쉽습니다.

### 3. 수정 횟수와 종료 조건을 정의합니다.

```text
NEEDS_REVISION
    ↓
수정 가능 횟수 확인
    ↓
Optimizer
    ↓
재평가
    ↓
종료 조건 확인
```

반복 횟수를 무제한으로 두지 않습니다.

LLM 호출 비용과 응답 시간을 고려하여 명시적인 상한을 설정해야 합니다.

### 4. 수정된 결과를 다시 검증합니다.

```text
Optimized Output
    ↓
기본 계약 검증
    ↓
의미적 품질 재평가
```

수정된 결과라고 해서 기존 검증을 생략해서는 안 됩니다.

### 5. 품질 문제가 해결되지 않았을 때의 정책을 정의합니다.

```text
품질 개선 실패
    ↓
최종 상태 결정
    ↓
출력 채택 여부 결정
    ↓
후속 처리 정책 적용
```

품질 문제가 해결되지 않았을 때 최초 결과를 유지할지, 대체 응답을 사용할지, 오류로 처리할지는 애플리케이션의 업무 요구사항에 따라 결정해야 합니다.

수업 12의 `NOT_RESOLVED`와 최초 출력 유지 정책은 Incident Responder 강의에서 사용하는 품질 통제 정책입니다.

실제 운영 환경에서는 품질 문제가 해결되지 않은 결과를 외부에 전달해도 되는지 별도의 정책이 필요할 수 있습니다.

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
→ 기술적으로 그럴듯한 AI 추천을
  그대로 외부에 전달해도 되는가?
→ Risk Control
```

수업 12에서는 LLM 출력의 의미적 품질을 평가하고 필요한 경우 개선합니다.

하지만 의미적 품질이 충분하다고 평가된 추천이라도 실제 시스템에 적용하면 위험할 수 있습니다.

예를 들어 다음과 같은 추천이 생성될 수 있습니다.

```text
Restart the order-service.

Rollback the latest deployment.

Delete obsolete database records.
```

이러한 추천은 특정 Incident를 해결하는 데 기술적으로 관련이 있을 수 있습니다.

하지만 실제 시스템에 적용할 경우 서비스 중단이나 데이터 손실 등의 위험이 발생할 수 있습니다.

따라서 의미적 품질 평가와 위험 추천 분류는 서로 다른 문제로 구분해야 합니다.

다음 수업에서는 AI가 생성한 추천의 위험 수준을 분류하고, 애플리케이션이 정한 정책에 따라 외부에 전달할 정보를 통제하는 방법을 다룹니다.

---

## 마무리

수업 11에서는 LLM 출력이 애플리케이션의 기본 계약을 충족하는지 검증했습니다.

하지만 기본 계약을 통과했다고 해서 출력 내용까지 신뢰할 수 있는 것은 아닙니다.

수업 12에서는 다음과 같은 품질 통제 구조를 추가합니다.

```text
LLM Output
    ↓
기본 계약 검증
    ↓
Evaluator
    ↓
PASS / NEEDS_REVISION
    ↓
필요하면 Optimizer 1회
    ↓
수정 결과 기본 계약 재검증
    ↓
Re-Evaluator
    ↓
PASSED / IMPROVED / NOT_RESOLVED
```

이 구조의 목적은 LLM이 생성한 모든 결과의 정확성을 보장하는 것이 아닙니다.

LLM의 평가와 수정 역시 확률적으로 동작합니다.

하지만 Java 애플리케이션은 평가 결과에 따라 어떤 흐름을 실행할지, 몇 번 수정할지, 어떤 결과를 채택할지, 언제 종료할지를 명시적으로 결정할 수 있습니다.

수업 12에서 가져가야 할 최종 메시지는 다음과 같습니다.

> **기본 계약을 통과한 출력이라도 의미적 품질까지 보장되는 것은 아닙니다.**

> **LLM에게 평가와 수정을 맡기더라도, 언제 수정하고 몇 번 수정하며 언제 종료할지는 Java 애플리케이션이 결정합니다.**

> **확률적인 AI의 판단을 결정론적인 애플리케이션 정책 안에서 관리하는 것이 품질 통제의 핵심입니다.**
