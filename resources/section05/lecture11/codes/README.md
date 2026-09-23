# lecture11-output-contract

수업 11에서 사용하는 코드는 수업 10까지의 Incident Responder에 **LLM 출력의 기본 계약 검증과 재시도(Retry), 대체 응답(Fallback) 처리를 추가하여, 모델이 생성한 결과를 애플리케이션이 정한 기준에 따라 처리하도록 변경한 코드**입니다.

수업 9에서는 요청 특성에 따라 실행 경로를 분리했고, 수업 10에서는 LLM에 전달하는 Context의 양을 제한했습니다.

하지만 입력 Context와 실행 경로를 통제하더라도 LLM이 항상 애플리케이션에서 요구하는 형태와 내용으로 응답한다는 보장은 없습니다.

예를 들어 다음과 같은 문제가 발생할 수 있습니다.

* JSON 형식이 올바르지 않아 Java 객체로 변환할 수 없는 경우
* 필수 필드가 누락되거나 빈 문자열로 반환되는 경우
* 추천 Action의 개수가 요구사항과 다른 경우
* 동일한 Action이 중복되는 경우
* Action의 priority가 허용된 범위를 벗어나는 경우

기존 코드에서는 이러한 문제가 발생해도 구조적으로 매핑할 수 있는 결과라면 그대로 다음 처리 단계로 전달될 수 있었습니다.

수업 11에서는 LLM의 응답을 그대로 신뢰하지 않고 애플리케이션이 정한 기본 계약에 따라 검증합니다.

```text
LLM Response
    ↓
JSON 형식 검증
    ↓
Java 객체 변환
    ↓
기본 출력 계약 검증
    ↓
┌──────────────────────┐
│ 검증 성공             │
│ → 정상 결과 반환      │
└──────────────────────┘

┌──────────────────────┐
│ 검증 실패             │
│ → 재시도              │
│ → 재검증              │
│ → 실패 시 Fallback    │
└──────────────────────┘
```

이 코드 스냅샷의 목적은 LLM이 언제나 올바른 결과를 생성하도록 만드는 것이 아닙니다.

**LLM이 어떤 결과를 생성하더라도 애플리케이션이 그 결과를 받아들일지, 다시 생성할지, 대체 응답을 사용할지 결정하도록 만드는 것**이 목적입니다.

> 모델 출력을 완전히 통제하는 것이 아니라, 어떤 출력이 와도 애플리케이션이 처리 방식을 통제합니다.

---

## 이 코드 스냅샷에서 달라진 것

수업 10과 비교해, 수업 11에서는 기존 Routing과 Context Budget 구조를 유지하면서 LLM 출력의 기본 계약을 검증하는 구조를 추가합니다.

| 항목             | 수업 10                               | 수업 11 변경                           |
| -------------- | ----------------------------------- | ---------------------------------- |
| Routing        | QUICK / STANDARD / CRITICAL         | 기존 구조 유지                           |
| Context Budget | Runbook, History, 외부 입력 및 재주입 크기 제한 | 기존 구조 유지                           |
| LLM 응답 처리      | 구조화 응답을 Java 객체로 변환                 | 응답 문자열 확보 후 형식 검증과 객체 변환을 명시적으로 분리 |
| JSON 형식        | 기존 구조화 응답 처리에 의존                    | JSON-only 검증 추가                    |
| DTO 기본 계약      | 별도 검증 부족                            | 필수값, 목록 개수, 중복 및 priority 검증       |
| 출력 변환          | 기존 ChatClient 변환 방식                 | `BeanOutputConverter`를 이용한 명시적 변환  |
| 출력 계약          | 변환과 검증의 책임이 명확하게 분리되지 않음            | Converter와 Validator의 책임 분리        |
| 실패 처리          | 기본적인 예외 처리                          | 계약 위반 시 재시도 및 Fallback 처리          |
| 정상 응답          | 매핑 가능한 결과 반환                        | 기본 계약을 통과한 결과 반환                   |
| 품질 확인          | 응답의 기본적인 사용 가능성 확인                  | 의도적인 계약 위반을 통해 검증 및 재시도 흐름 확인      |

수업 11은 수업 7~10에서 만든 관찰 구조와 비용 통제 구조를 그대로 사용합니다.

기존 애플리케이션을 다시 만드는 것이 아니라, **LLM 출력이 애플리케이션 내부로 들어오는 경계에 검증과 실패 처리 정책을 추가**합니다.

---

## 수업 11 학습 포인트

수업 11에서는 다음 내용을 확인합니다.

* LLM이 JSON 응답을 생성하도록 요청해도 항상 올바른 JSON을 반환한다고 보장할 수 없다는 점
* JSON을 Java DTO 또는 record로 변환할 수 있다는 사실과 응답이 업무 요구사항을 충족한다는 사실은 다르다는 점
* `entity(Class)`를 사용하더라도 내부적으로 출력 변환 과정이 필요하다는 점
* `content()`를 통해 LLM의 원본 응답 문자열을 확보하고 검증 및 변환 과정을 명시적으로 분리하는 방법
* `JsonOnlyResponseValidator`로 JSON 형식 요구사항을 확인하는 방법
* `BeanOutputConverter`를 이용해 응답 문자열을 Java 객체로 변환하는 방법
* Converter와 Validator를 별도의 책임으로 분리하는 이유
* Analysis와 Action에 서로 다른 출력 계약을 적용하는 방법
* 필수 필드, 빈 문자열, 목록 개수, 중복 및 priority 범위를 검증하는 방법
* 기본 계약을 위반한 결과에 대해 재시도를 수행하는 방법
* 재시도 후에도 계약을 충족하지 못하면 Fallback으로 처리하는 방법
* JSON 매핑 실패와 DTO 계약 위반을 구분하는 이유
* 외부 LLM 호출 오류와 출력 계약 오류를 구분하여 처리하는 방법
* 의도적으로 잘못된 응답을 유도하여 품질 통제 흐름을 관찰하는 방법
* 확률적인 LLM 출력에 대해 결정론적인 애플리케이션 처리 정책을 적용하는 방법

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section05/
    lecture11/
      codes/
        README.md
        lecture11-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

`lecture11-incident-responder/` 디렉터리가 실제 Spring Boot 애플리케이션 프로젝트 루트입니다.

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

`lecture11-incident-responder/` 프로젝트 루트에서 다음 명령을 실행합니다.

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

이 요청은 기존 Routing 정책에 따라 `STANDARD_ANALYSIS` 경로로 처리됩니다.

```text
Incident Request
    ↓
STANDARD_ANALYSIS
    ↓
Analysis LLM
    ↓
Analysis 출력 계약 검증
    ↓
Action LLM
    ↓
Action 출력 계약 검증
    ↓
Incident Response
```

수업 11에서는 기존 Routing 정책을 변경하지 않습니다.

동일한 Incident 요청을 사용하면서 LLM 응답의 기본 계약을 검증하는 과정에 집중합니다.

---

## 수업 11에서 해결하려는 문제

### 1. Java 객체로 변환할 수 있어도 올바른 응답은 아닐 수 있습니다.

예를 들어 Analysis LLM이 다음과 같은 JSON을 반환했다고 가정합니다.

```json
{
  "situationSummary": "Order API errors are increasing.",
  "likelyCauses": [
    "Database connection pool saturation"
  ],
  "impactAssessment": "Order processing may be delayed.",
  "confidenceNote": ""
}
```

JSON 문법에는 문제가 없습니다.

Java 객체로 변환하는 과정에서도 오류가 발생하지 않을 수 있습니다.

하지만 `confidenceNote`는 빈 문자열입니다.

애플리케이션에서 이 필드를 필수값으로 요구한다면 해당 응답은 기본 계약을 위반한 결과입니다.

```text
JSON 문법 정상
    ↓
Java 객체 변환 성공
    ↓
confidenceNote = ""
    ↓
기본 계약 위반
```

따라서 Java 객체 변환에 성공했다는 이유만으로 결과를 그대로 받아들여서는 안 됩니다.

### 2. Action의 개수와 priority가 잘못될 수 있습니다.

Action LLM에 정확히 3개의 추천 Action을 요청했다고 가정합니다.

하지만 실제 응답에는 2개의 Action만 포함될 수 있습니다.

```text
요구사항
→ Action 3개

실제 응답
→ Action 2개

결과
→ 기본 계약 위반
```

또는 Action이 3개이더라도 priority가 다음과 같을 수 있습니다.

```text
Action 1 → priority 1
Action 2 → priority 1
Action 3 → priority 4
```

수업 11의 기본 계약에서 priority는 1~3이며 서로 중복되지 않아야 합니다.

따라서 이 결과 역시 계약 위반입니다.

### 3. JSON 형식 자체가 잘못될 수 있습니다.

LLM이 JSON 앞뒤에 설명 문장을 추가하거나 코드 블록을 포함하여 응답할 수도 있습니다.

```text
Here is the analysis:

{
  "situationSummary": "...",
  ...
}
```

이러한 응답은 애플리케이션이 요구하는 JSON-only 형식과 다릅니다.

수업 11에서는 응답 문자열의 형식 검증과 Java 객체 변환을 별도의 처리 단계로 구분합니다.

---

## 품질 통제 전과 후

### Before

```text
Incident Request
    ↓
Analysis LLM
    ↓
Java 객체 변환
    ↓
Analysis 결과 사용
    ↓
Action LLM
    ↓
Java 객체 변환
    ↓
Action 결과 사용
```

이 구조에서는 객체 변환에 성공하더라도 필수값이나 목록 개수 등 업무상 요구하는 기본 계약을 충족하지 못할 수 있습니다.

### After

```text
Incident Request
    ↓
Analysis LLM
    ↓
JSON 형식 검증
    ↓
Analysis 객체 변환
    ↓
Analysis 기본 계약 검증
    ↓
검증 성공 / 실패 분기
    ↓
Action LLM
    ↓
JSON 형식 검증
    ↓
Action 객체 변환
    ↓
Action 기본 계약 검증
    ↓
검증 성공 / 실패 분기
    ↓
Incident Response
```

검증에 실패하면 정해진 재시도 정책을 적용합니다.

재시도 후에도 정상적인 결과를 확보하지 못하면 Fallback으로 처리합니다.

---

## LLM 응답 처리의 책임 분리

수업 11에서는 LLM 응답 처리 과정을 크게 세 가지 책임으로 구분합니다.

| 책임                | 역할                               |
| ----------------- | -------------------------------- |
| JSON 형식 검증        | 응답 문자열이 JSON-only 요구사항을 충족하는지 확인 |
| 출력 변환(Conversion) | 응답 문자열을 Java 객체로 변환              |
| 출력 검증(Validation) | 변환된 객체가 애플리케이션의 기본 계약을 충족하는지 확인  |

각 단계가 해결하는 문제는 서로 다릅니다.

```text
JSON 형식 검증
→ 응답 형식이 올바른가?

출력 변환
→ Java 객체로 변환할 수 있는가?

출력 검증
→ 변환된 객체가 기본 계약을 충족하는가?
```

이러한 책임 분리를 통해 JSON 형식 오류, 객체 변환 오류, 기본 계약 위반을 구분하여 처리할 수 있습니다.

---

## JsonOnlyResponseValidator

`JsonOnlyResponseValidator`는 LLM의 원본 응답 문자열이 애플리케이션에서 요구하는 JSON 형식에 부합하는지 확인합니다.

예를 들어 다음과 같은 응답은 JSON-only 요구사항에 부합합니다.

```json
{
  "situationSummary": "Order API errors are increasing."
}
```

반면 다음과 같이 JSON 외부에 설명 문장이 포함된 응답은 요구사항을 위반합니다.

```text
The analysis result is:

{
  "situationSummary": "Order API errors are increasing."
}
```

JSON-only 검증은 출력 내용의 품질을 평가하는 단계가 아닙니다.

응답 문자열이 정해진 형식으로 반환되었는지 확인하는 단계입니다.

> JSON 형식 검증에 성공하더라도 Java 객체 변환과 기본 계약 검증은 별도로 수행해야 합니다.

---

## BeanOutputConverter

`BeanOutputConverter`는 LLM이 반환한 JSON 문자열을 Java 객체로 변환하는 역할을 합니다.

기존의 `entity(Class)` 방식에서는 구조화된 응답을 Java 객체로 변환하는 과정이 ChatClient 내부에 포함되어 있었습니다.

수업 11에서는 `content()`를 통해 응답 문자열을 확보한 후 변환 과정을 명시적으로 처리합니다.

```text
ChatClient
    ↓
content()
    ↓
LLM 응답 문자열
    ↓
JsonOnlyResponseValidator
    ↓
BeanOutputConverter
    ↓
Java 객체
    ↓
IncidentAiOutputValidator
```

이 구조를 통해 객체 변환 이전의 응답 문자열을 검증할 수 있고, 객체 변환과 기본 계약 검증의 책임도 분리할 수 있습니다.

`BeanOutputConverter`는 JSON을 Java 객체로 변환하는 도구입니다.

필수 필드의 내용이 적절한지, Action이 정확히 3개인지, priority가 중복되지 않는지와 같은 기본 계약을 모두 보장하는 도구는 아닙니다.

---

## IncidentAiOutputContract

`IncidentAiOutputContract`는 LLM 출력의 변환과 검증에 필요한 공통 계약을 정의합니다.

이 계약을 통해 출력 처리 과정에서 사용하는 Converter와 Validator의 역할을 구분합니다.

```text
IncidentAiOutputContract
    ├── Converter
    │     └── LLM 응답 문자열을 Java 객체로 변환
    │
    └── Validator
          └── 변환된 Java 객체의 기본 계약 검증
```

Analysis와 Action은 서로 다른 출력 구조와 검증 조건을 가지고 있습니다.

따라서 공통적인 출력 처리 흐름은 유지하되, 실제 변환 대상과 검증 규칙은 각각의 출력 유형에 맞게 적용합니다.

---

## IncidentAiOutputConverter

`IncidentAiOutputConverter`는 LLM 응답 문자열을 애플리케이션에서 사용하는 출력 객체로 변환하는 책임을 담당합니다.

출력 변환 과정에서는 다음과 같은 문제가 발생할 수 있습니다.

* JSON 문법 오류
* Java 객체와 JSON 필드 구조의 불일치
* 필드 타입 불일치
* 필수적인 객체 구조의 변환 실패

이러한 오류는 출력 객체의 기본 계약을 검증하기 이전에 발생하는 문제입니다.

따라서 객체 변환 실패와 변환된 객체의 계약 위반을 구분하여 처리합니다.

---

## IncidentAiOutputValidator

`IncidentAiOutputValidator`는 변환된 Java 객체가 애플리케이션에서 요구하는 기본 계약을 충족하는지 확인합니다.

수업 11에서는 Analysis와 Action에 서로 다른 검증 조건을 적용합니다.

### Analysis 기본 계약

Analysis 결과는 다음 네 가지 필드를 포함합니다.

```text
situationSummary
likelyCauses
impactAssessment
confidenceNote
```

검증 대상은 다음과 같습니다.

| 항목               | 검증 내용                      |
| ---------------- | -------------------------- |
| Analysis 객체      | null 여부                    |
| situationSummary | 필수값 및 빈 문자열 여부             |
| likelyCauses     | 필수 목록, 최소 개수, 빈 항목 및 중복 여부 |
| impactAssessment | 필수값 및 빈 문자열 여부             |
| confidenceNote   | 필수값 및 빈 문자열 여부             |

예를 들어 `confidenceNote`가 빈 문자열이면 JSON 변환에 성공하더라도 기본 계약 검증에는 실패합니다.

### Action 기본 계약

Action 결과에서는 다음 조건을 확인합니다.

| 항목          | 검증 내용                         |
| ----------- | ----------------------------- |
| Action 목록   | 정확히 3개인지 확인                   |
| Action 필수값  | 필수 필드가 누락되거나 비어 있는지 확인        |
| Action 중복   | 동일한 추천 Action이 중복되는지 확인       |
| priority    | 1~3 범위인지 확인                   |
| priority 중복 | 각 Action의 priority가 서로 다른지 확인 |

예를 들어 다음과 같은 priority는 정상입니다.

```text
1, 2, 3
```

다음과 같은 priority는 기본 계약을 위반합니다.

```text
1, 1, 3
1, 2, 4
```

이 검증은 Action이 실제 Incident를 해결하는 데 적절한지 판단하는 과정과는 다릅니다.

**수업 11에서는 출력의 기본 계약을 검증하고, 수업 12에서는 출력 내용의 의미적 품질을 평가합니다.**

---

## ValidationResult

`ValidationResult`는 출력 검증 결과를 표현하는 객체입니다.

검증 결과를 단순한 boolean 값으로만 반환하면 실패 이유를 전달하기 어렵습니다.

```text
true
→ 검증 성공

false
→ 검증 실패
→ 어떤 조건을 위반했는지 알기 어려움
```

따라서 검증 결과와 실패 이유를 함께 표현합니다.

```text
ValidationResult
    ↓
검증 성공 여부
    ↓
계약 위반 사유
```

대표적인 사용 의도는 다음과 같습니다.

```java
ValidationResult.success();
```

```java
ValidationResult.failure(...);
```

```java
ValidationResult.from(...);
```

`success()`는 검증에 성공했음을 나타내고, `failure()`는 검증 실패를 표현합니다.

`from()`은 검증 과정에서 확인한 조건을 바탕으로 결과를 생성하는 데 사용합니다.

이러한 구조를 통해 호출자는 검증 결과를 확인하고 정상 처리 또는 재시도 여부를 결정할 수 있습니다.

---

## IncidentChatGateway

`IncidentChatGateway`는 Analysis와 Action의 LLM 호출 및 응답 처리 경계를 담당합니다.

수업 11에서는 LLM 응답을 바로 Java 객체로 사용하는 대신, 원본 응답 문자열을 확보하여 검증과 변환 과정을 명시적으로 수행합니다.

```text
IncidentChatGateway
    ↓
Analysis LLM 호출
    ↓
원본 응답 문자열
    ↓
JSON 형식 검증
    ↓
Analysis 객체 변환
    ↓
Analysis 기본 계약 검증
```

Action도 동일한 처리 원칙을 적용합니다.

```text
IncidentChatGateway
    ↓
Action LLM 호출
    ↓
원본 응답 문자열
    ↓
JSON 형식 검증
    ↓
Action 객체 변환
    ↓
Action 기본 계약 검증
```

Analysis와 Action은 서로 다른 Prompt와 출력 계약을 사용하지만, 출력 처리의 기본 원칙은 동일합니다.

---

## 출력 계약 위반 시 재시도

LLM이 기본 계약을 위반한 응답을 생성했다고 해서 해당 결과를 즉시 최종 응답으로 사용하지 않습니다.

수업 11에서는 검증 실패 시 정해진 횟수 안에서 재시도를 수행합니다.

```text
LLM 호출
    ↓
응답 검증
    ↓
계약 위반
    ↓
수정 요청 Prompt
    ↓
LLM 재호출
    ↓
응답 재검증
```

재시도는 무제한으로 수행하지 않습니다.

재시도 횟수에 상한이 없다면 응답 시간과 LLM 호출 비용이 예상보다 증가할 수 있기 때문입니다.

따라서 재시도 역시 애플리케이션이 정한 정책에 따라 수행합니다.

### 재시도 후 정상 응답을 확보한 경우

```text
첫 번째 LLM 응답
    ↓
기본 계약 위반
    ↓
재시도
    ↓
두 번째 LLM 응답
    ↓
기본 계약 통과
    ↓
정상 결과 반환
```

### 재시도 후에도 계약을 위반한 경우

```text
첫 번째 LLM 응답
    ↓
기본 계약 위반
    ↓
재시도
    ↓
두 번째 LLM 응답
    ↓
기본 계약 위반
    ↓
Fallback 처리
```

핵심은 모델이 두 번째 호출에서 반드시 올바른 응답을 생성한다고 가정하지 않는 것입니다.

---

## JSON 매핑 실패와 계약 위반 구분

LLM 출력 처리 과정에서 발생하는 실패는 모두 같은 문제가 아닙니다.

### JSON 매핑 실패

```text
LLM Response
    ↓
JSON 형식 오류 또는 객체 변환 실패
    ↓
AiOutputMappingException
```

JSON 문법이나 객체 구조가 올바르지 않아 Java 객체를 확보하지 못한 경우입니다.

### 기본 계약 위반

```text
LLM Response
    ↓
Java 객체 변환 성공
    ↓
Validator
    ↓
기본 계약 위반
```

Java 객체를 확보했지만 애플리케이션에서 정한 필수값, 목록 개수 또는 priority 조건을 충족하지 못한 경우입니다.

두 문제를 구분하면 실패 원인에 맞는 처리 흐름을 구성할 수 있습니다.

---

## 외부 호출 오류와 출력 계약 오류 구분

LLM 호출 과정에서는 출력 내용과 관계없는 오류도 발생할 수 있습니다.

예를 들어 다음과 같은 상황입니다.

```text
Provider 오류
네트워크 오류
인증 오류
요청 제한
```

이러한 오류는 모델이 생성한 결과의 기본 계약을 위반한 상황과 다릅니다.

따라서 애플리케이션은 다음 두 종류의 실패를 구분하여 처리합니다.

```text
LLM 호출 자체의 실패
→ 외부 호출 오류 처리

LLM 호출 성공 후 출력 계약 위반
→ 검증 및 재시도 정책 적용
```

수업 11에서는 이러한 차이를 예외 처리 구조를 통해 확인합니다.

---

## Fallback 처리

재시도를 수행하더라도 정상적인 LLM 출력을 확보하지 못할 수 있습니다.

이때 애플리케이션이 정해진 대체 응답을 사용하도록 처리하는 것이 Fallback입니다.

```text
LLM 호출
    ↓
출력 계약 검증
    ↓
검증 실패
    ↓
재시도
    ↓
재검증 실패
    ↓
Fallback
```

Fallback은 모델이 올바른 응답을 생성하도록 만드는 기능이 아닙니다.

모델의 응답을 사용할 수 없는 상황에서 애플리케이션이 정해진 방식으로 실패를 처리하기 위한 장치입니다.

Fallback 응답 역시 애플리케이션이 요구하는 기본 출력 구조를 충족해야 합니다.

다만 기본 계약을 충족한다는 이유만으로 실제 Incident에 대한 분석이나 추천 내용의 정확성이 보장되는 것은 아닙니다.

---

## 품질 통제 실험

수업 11에서는 LLM이 기본 계약을 위반하는 상황을 의도적으로 유도하여 검증과 재시도 흐름을 확인합니다.

실험에 사용하는 실패 유도 Prompt는 강의용입니다.

실제 운영 환경에서는 이러한 실패 유도 문구를 제거해야 합니다.

또한 LLM은 확률적으로 동작하므로 동일한 Prompt를 사용하더라도 매번 의도한 실패가 발생한다고 보장할 수 없습니다.

### 실험 1. Analysis 필수값 검증

첫 번째 Analysis 응답에서 `confidenceNote`가 빈 문자열이 되도록 유도합니다.

```text
첫 번째 Analysis LLM
    ↓
confidenceNote = ""
    ↓
Validator
    ↓
필수값 계약 위반
    ↓
retryAnalysis()
    ↓
두 번째 Analysis LLM
    ↓
기본 계약 재검증
```

재시도 결과가 기본 계약을 충족하면 정상적인 Analysis 결과로 사용합니다.

### 실험 2. Action 개수 검증

첫 번째 Action 응답에서 Action을 2개만 생성하도록 유도합니다.

```text
첫 번째 Action LLM
    ↓
Action 2개
    ↓
Validator
    ↓
목록 개수 계약 위반
    ↓
재시도
    ↓
Action 3개 생성 유도
    ↓
기본 계약 재검증
```

재시도 후 정확히 3개의 Action이 생성되고 나머지 계약도 충족하면 정상 결과로 사용합니다.

### 실험 3. Action priority 검증

Action의 priority가 중복되거나 허용 범위를 벗어나는 상황을 유도합니다.

```text
첫 번째 Action LLM
    ↓
priority = 1, 1, 4
    ↓
Validator
    ↓
priority 계약 위반
    ↓
재시도
    ↓
priority = 1, 2, 3 생성 유도
    ↓
기본 계약 재검증
```

priority의 범위와 중복 여부를 모두 확인합니다.

### 실험 4. JSON-only 검증

LLM이 JSON 외부에 설명 문장이나 코드 블록을 추가하는 상황을 유도합니다.

```text
LLM Response
    ↓
JSON 외부 텍스트 포함
    ↓
JsonOnlyResponseValidator
    ↓
형식 계약 위반
    ↓
재시도
    ↓
JSON-only 응답 생성 유도
```

JSON-only 검증을 통과하더라도 Java 객체 변환과 기본 계약 검증을 별도로 수행합니다.

---

## 실험 결과를 해석할 때 주의할 점

수업 11의 실패 유도 Prompt는 의도한 오류를 재현하기 위한 강의용 장치입니다.

하지만 LLM이 항상 지시한 대로 잘못된 응답을 생성하는 것은 아닙니다.

예를 들어 Action을 2개만 반환하도록 요청하더라도 모델이 처음부터 3개의 Action을 생성할 수 있습니다.

이 경우 첫 번째 응답이 기본 계약을 충족한다면 재시도가 발생하지 않는 것이 정상입니다.

```text
실패 유도 Prompt
    ↓
LLM이 정상 응답 생성
    ↓
Validator 통과
    ↓
재시도 없이 정상 처리
```

반대로 의도한 실패가 발생했다면 검증과 재시도 흐름을 확인할 수 있습니다.

```text
실패 유도 Prompt
    ↓
LLM이 계약 위반 응답 생성
    ↓
Validator 실패
    ↓
재시도
    ↓
재검증
```

실험의 핵심은 모델이 반드시 실패하도록 만드는 것이 아닙니다.

**모델이 정상 응답을 생성하면 정상 처리하고, 계약을 위반하면 애플리케이션이 정해진 실패 처리 정책을 적용하는지 확인하는 것**입니다.

---

## 핵심 코드 위치

수업 11에서 새로 추가하거나 중요하게 변경되는 주요 클래스는 다음과 같습니다.

| 클래스                         | 책임                                |
| --------------------------- | --------------------------------- |
| `JsonOnlyResponseValidator` | LLM 원본 응답의 JSON-only 형식 검증        |
| `IncidentAiOutputContract`  | 출력 변환과 검증을 위한 공통 계약               |
| `IncidentAiOutputConverter` | LLM 응답 문자열을 출력 객체로 변환             |
| `IncidentAiOutputValidator` | 변환된 객체의 기본 출력 계약 검증               |
| `ValidationResult`          | 검증 성공 여부와 실패 사유 표현                |
| `AiOutputMappingException`  | 출력 매핑 실패 표현                       |
| `IncidentChatGateway`       | Analysis 및 Action LLM 호출과 출력 처리   |
| `IncidentPromptFactory`     | 기존 Prompt 구성 및 재시도에 필요한 Prompt 생성 |
| `IncidentResponseWorkflow`  | 기존 Incident 처리 흐름과 출력 처리 결과 연결    |

`BeanOutputConverter`는 Spring AI에서 제공하는 출력 변환 도구로, LLM 응답을 Java 객체로 변환하는 데 사용합니다.

기존 수업에서 구현한 Routing, Context Budget, Observability 관련 클래스는 유지합니다.

---

## 정상 흐름 확인

애플리케이션 실행 후 다음 항목을 순서대로 확인합니다.

1. 애플리케이션이 정상 기동되는지 확인합니다.
2. 기존 Incident API가 정상적으로 호출되는지 확인합니다.
3. STANDARD 요청이 기존 Routing 정책에 따라 처리되는지 확인합니다.
4. Analysis LLM의 응답 문자열을 확보하는지 확인합니다.
5. JSON-only 검증을 수행하는지 확인합니다.
6. Analysis 객체 변환을 수행하는지 확인합니다.
7. Analysis 기본 계약을 검증하는지 확인합니다.
8. 기본 계약을 충족하면 정상적인 Analysis 결과를 사용하는지 확인합니다.
9. Action LLM 호출 후 JSON 형식 검증과 객체 변환을 수행하는지 확인합니다.
10. Action 목록 개수, 필수값, 중복 및 priority를 검증하는지 확인합니다.
11. 기본 계약을 충족하면 정상적인 Action 결과를 반환하는지 확인합니다.
12. 계약 위반 응답을 유도했을 때 재시도 정책이 적용되는지 확인합니다.
13. 재시도 후에도 계약을 충족하지 못하면 Fallback으로 처리하는지 확인합니다.
14. 기존 Routing과 Context Budget 구조가 유지되는지 확인합니다.

---

## 수업 11에서 확인하는 범위

이번 수업에서는 다음 내용을 다룹니다.

```text
- LLM 출력의 JSON-only 형식 검증
- Java 객체 변환
- Converter와 Validator 책임 분리
- Analysis 기본 계약 검증
- Action 기본 계약 검증
- 필수값 및 빈 문자열 검증
- 목록 개수 검증
- 중복 검증
- priority 범위 및 중복 검증
- ValidationResult
- JSON 매핑 실패와 계약 위반 구분
- 기본 계약 위반 시 재시도
- 재시도 후 재검증
- 재시도 실패 시 Fallback
- 강의용 실패 유도 Prompt
- 정상 및 실패 처리 흐름 확인
```

이번 수업에서는 다음 내용을 다루지 않습니다.

```text
- LLM 출력의 근거 일치성 평가
- 분석 내용의 구체성 평가
- 과도한 확신 평가
- 추천 Action의 의미적 적절성 평가
- Evaluator–Optimizer
- LLM 출력의 사실 정확성 보장
- 위험 추천 분류
- riskLevel / approvalRequired / riskReason
- 실제 Tool 실행
- 관리자 승인
- 자동 복구
```

이번 수업의 검증 범위는 출력의 기본 계약입니다.

기본 계약을 통과한 결과가 실제 Incident에 대한 올바른 분석이나 적절한 추천이라는 의미는 아닙니다.

---

## 다른 AI 애플리케이션에서 출력 계약을 설계할 때 확인할 것

수업 11의 검증 조건을 다른 애플리케이션에 그대로 적용하는 것이 목적은 아닙니다.

각 애플리케이션의 업무 요구사항에 따라 필요한 출력 계약을 정의해야 합니다.

### 1. 출력 형식을 정의합니다.

```text
JSON
    ↓
필수 필드
    ↓
필드 타입
    ↓
객체 및 목록 구조
```

LLM 응답을 애플리케이션에서 어떤 형태로 사용할 것인지 먼저 정의합니다.

### 2. 기본 계약을 정의합니다.

```text
필수값
빈 문자열
목록 개수
중복
허용 범위
```

단순히 Java 객체로 변환할 수 있는지를 넘어 애플리케이션이 반드시 요구하는 조건을 정의합니다.

### 3. 실패 처리 정책을 정의합니다.

```text
출력 계약 위반
    ↓
재시도 가능 여부
    ↓
최대 재시도 횟수
    ↓
재시도 실패 시 처리 방식
```

재시도 횟수는 무제한으로 두지 않습니다.

LLM 호출 비용과 처리 시간을 고려하여 명시적인 상한을 설정해야 합니다.

### 4. 형식 검증과 의미적 품질 평가를 구분합니다.

```text
기본 계약 검증
→ 애플리케이션이 요구하는 구조와 필수 조건을 충족하는가?

의미적 품질 평가
→ 실제 입력과 근거에 비추어 내용이 적절한가?
```

두 단계는 서로 다른 문제를 해결합니다.

기본 계약을 충족하는 응답이라도 실제 업무에 사용하기에는 부적절할 수 있습니다.

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
→ LLM 출력이 애플리케이션의 기본 계약을 지키는가?
→ Quality Control

수업 12
→ 기본 계약을 통과한 출력의 내용도 신뢰할 수 있는가?
→ Quality Control
```

수업 11에서는 JSON 형식과 Java 객체 변환, 필수값, 목록 개수, 중복 및 priority를 검증합니다.

하지만 기본 계약을 모두 통과하더라도 다음과 같은 문제가 남아 있을 수 있습니다.

```text
Incident와 관계없는 원인 분석

제공된 근거와 일치하지 않는 설명

불확실한 상황에 대한 과도한 확신

실제 장애 상황에 적절하지 않은 추천 Action
```

이러한 문제는 단순한 DTO 계약 검증만으로 해결하기 어렵습니다.

다음 수업에서는 출력의 의미적 품질을 평가하고, 문제가 발견된 결과를 개선하는 Evaluator–Optimizer 구조를 다룹니다.

---

## 마무리

수업 11 이전에는 LLM이 생성한 결과를 Java 객체로 변환하여 애플리케이션에서 사용했습니다.

하지만 객체 변환에 성공했다는 사실만으로 애플리케이션이 요구하는 기본 계약을 충족했다고 볼 수는 없습니다.

수업 11에서는 다음과 같은 처리 구조를 추가합니다.

```text
LLM Response
    ↓
JSON 형식 검증
    ↓
Java 객체 변환
    ↓
기본 계약 검증
    ↓
정상 처리 / 재시도
    ↓
재검증
    ↓
정상 처리 / Fallback
```

이 구조의 목적은 LLM의 출력을 완전히 결정론적으로 만드는 것이 아닙니다.

LLM은 여전히 확률적으로 동작하며, 동일한 입력에서도 서로 다른 결과를 생성할 수 있습니다.

하지만 애플리케이션은 어떤 결과를 받아들일지, 어떤 결과를 거부할지, 언제 재시도할지, 재시도에 실패하면 어떻게 처리할지를 명시적으로 결정할 수 있습니다.

수업 11에서 가져가야 할 최종 메시지는 다음과 같습니다.

> **Java 객체로 변환할 수 있다는 사실은 출력의 기본 계약을 보장하지 않습니다.**

> **LLM 출력은 애플리케이션이 정한 계약에 따라 검증하고, 계약을 위반하면 정해진 정책으로 처리해야 합니다.**

> **모델 출력을 완전히 통제하는 것이 아니라, 어떤 출력이 와도 애플리케이션이 처리 방식을 통제합니다.**
