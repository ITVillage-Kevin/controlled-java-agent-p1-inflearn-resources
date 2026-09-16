# lecture10-context-budget

수업 10에서 사용하는 코드는 수업 9까지의 Incident Responder에 **Context Budget을 추가해, 선택된 AI 처리 경로 안에서 LLM에 전달되는 Context의 양을 애플리케이션이 직접 통제하도록 변경한 코드**입니다.

수업 7에서는 요청 흐름을 trace/span/event로 관찰할 수 있게 만들었고, 수업 8에서는 그 관찰 데이터를 Langfuse로 전송해 실제 Prompt와 token usage를 확인했습니다. 수업 9에서는 `QUICK_TRIAGE`, `STANDARD_ANALYSIS`, `CRITICAL_ESCALATION`으로 실행 경로를 분리했습니다.

수업 10에서는 이제 `STANDARD_ANALYSIS`와 `CRITICAL_ESCALATION`처럼 실제 LLM을 사용하는 경로 안에서 **어떤 Context를 얼마나 허용할 것인지**를 통제합니다.

기존 코드는 관련 Runbook이나 과거 Incident 이력이 여러 개 검색되면 그 결과를 모두 Prompt에 전달했습니다. 또한 외부 Incident 입력과 첫 번째 LLM의 분석 결과도 별도의 크기 상한 없이 다음 Prompt에 들어갈 수 있었습니다.

수업 10에서는 Context의 성격에 따라 통제 방법을 나눕니다.

```text
Runbook / History
→ selection budget
→ 무엇을 몇 개까지 보낼지 통제

Incident 외부 입력 / 이전 LLM output
→ size budget
→ 얼마나 길게 허용할지 통제
```

이 코드 스냅샷의 목적은 가장 복잡한 Context 최적화 알고리즘을 만드는 것이 아닙니다. **관련 데이터는 찾되, LLM에 전달할 양에는 명시적인 상한을 두고, 실제 input token 변화를 다시 관찰하는 것**이 목적입니다.

> **관련 데이터는 찾고, 전달량은 통제합니다.**

---

## 이 코드 스냅샷에서 달라진 것

수업 9 코드와 비교해, 수업 10에서는 Routing 구조를 그대로 유지하면서 AI 처리 경로 내부의 Context 전달량을 통제합니다.

| 항목              | 수업 9                          | 수업 10 변경                                      |
| --------------- | ----------------------------- | --------------------------------------------- |
| Routing         | QUICK / STANDARD / CRITICAL   | 기존 구조 유지                                      |
| Runbook 검색      | 관련 후보를 정렬 후 모두 반환             | `deduplicationKey` 중복 제거 + severity별 Top N    |
| History 검색      | 유사도가 있는 이력을 정렬 후 모두 반환        | 기존 similarity 정렬 유지 + severity별 Top N         |
| Runbook budget  | 없음                            | `RunbookContextBudget` 추가                     |
| History budget  | 없음                            | `HistoryContextBudget` 추가                     |
| 외부 Incident 입력  | 별도 크기 상한 없음                   | `title`, `summary`, `symptoms` 등에 문자 수 상한 적용  |
| Analysis 결과 재주입 | 1차 LLM 결과를 2차 Prompt에 그대로 재주입 | field별 문자 수 상한 적용                             |
| 최종 Prompt       | 조립 후 그대로 호출                   | 고정 instruction을 보존하고 전체 크기는 safety validation |
| 관찰              | route별 trace 확인               | 같은 STANDARD 요청의 Before / After input token 비교 |
| 성공 기준           | 실행 경로가 실제로 달라지는지 확인           | Context 감소가 실제 input token 감소로 이어지는지 확인       |

수업 10은 수업 7~9에서 만든 Micrometer Observation, OpenTelemetry, OTLP exporter, Langfuse 연결을 그대로 사용합니다.

관찰 구조를 다시 만드는 것이 아니라, **비용 통제 전후를 동일한 관찰 구조에서 비교**합니다.

---

## 수업 10 학습 포인트

수업 10에서는 이 프로젝트를 통해 아래 내용을 확인합니다.

* 데이터가 축적될수록 관련 Context 후보도 증가할 수 있다는 점
* 관련 데이터를 찾는 것과 관련 데이터를 모두 LLM에 전달하는 것은 다른 문제라는 점
* Runbook/History처럼 이미 작은 의미 단위로 준비된 Context는 무조건 문자열을 자르기보다 **선택량을 통제**하는 편이 적절할 수 있다는 점
* `deduplicationKey`로 같은 문제군의 Runbook 중복을 줄이는 방식
* priority / updatedAt 정렬 후 deduplication을 적용하는 이유
* deduplication 후 severity별 Top N을 적용하는 이유
* History는 기존 similarity score와 최신성 정렬을 유지한 채 Top N만 적용하는 방식
* 외부 Incident 입력에는 문자 수 기반 hard limit을 적용하는 이유
* 첫 번째 LLM의 output도 두 번째 LLM 호출에서는 다시 input 비용이 된다는 점
* Analysis 결과의 각 field에 별도 상한을 두어 재주입 크기를 제한하는 방식
* 최종 Prompt 전체를 마지막에 무조건 truncate하지 않는 이유
* 같은 STANDARD 요청을 개선 전후 실행해 Langfuse에서 input token을 비교하는 방법
* input token 감소율과 실제 전체 비용 절감률을 구분해야 하는 이유
* latency를 한두 번의 실행 결과만으로 성공 기준으로 삼지 않는 이유
* 비용 통제를 `관찰 → 정책 적용 → 재관찰` 과정으로 이해하는 방법

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section04/
    lecture10/
      codes/
        README.md
        lecture10-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture10-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

> 실제 저장소의 Section 디렉터리 번호가 다르다면 현재 강의 자료 구조에 맞게 경로만 조정합니다.

---

## 실행 환경

* Java 21
* Spring Boot 3.5.13
* Spring AI 1.1.4
* Gradle
* OpenAI API Key
* Langfuse Japan project 사전 준비 완료
* 수업 8의 OTLP / Langfuse 환경 변수 설정 유지

### 실행 전 준비

애플리케이션 실행에는 OpenAI API Key가 필요합니다.

```bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

Langfuse에서 Prompt와 token usage를 확인하려면 수업 8에서 사용한 아래 환경 변수도 준비되어 있어야 합니다.

```text
LANGFUSE_OTLP_TRACES_ENDPOINT
LANGFUSE_OTLP_AUTHORIZATION
```

실제 key, secret, Authorization 값은 코드, `application.yml`, Git, 로그에 직접 기록하지 않습니다.

---

## 실행 방법

### 1) 터미널에서 실행

`lecture10-incident-responder/` 프로젝트 루트에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

애플리케이션 실행 후 OpenAI API 또는 Langfuse exporter 관련 오류가 없는지 콘솔을 먼저 확인합니다.

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

수업 10의 Before / After 비교에는 아래 STANDARD 요청을 고정해서 사용합니다.

```json
{
  "sourceSystem": "order-service",
  "title": "Order API HTTP 500 and database timeout increase",
  "summary": "Order API HTTP 500 errors are increasing with database timeout, connection pool saturation, slow transactions, lock timeouts, and retry spikes after a recent deployment.",
  "severity": "HIGH",
  "symptoms": [
    "HTTP 500",
    "database timeout",
    "connection pool saturation",
    "slow transaction",
    "lock timeout",
    "retry spike",
    "recent deployment"
  ]
}
```

이 요청은 다음 이유로 `STANDARD_ANALYSIS`를 사용합니다.

```text
severity = HIGH
→ CRITICAL severity 아님

order-service
→ QUICK notification-delay 조건 아님

명확한 CRITICAL signal 없음
→ STANDARD_ANALYSIS
```

따라서 개선 전후 모두 다음 조건을 유지할 수 있습니다.

```text
같은 request
같은 route
같은 workflow
같은 두 LLM 호출
```

즉 Context 통제 효과만 비교하기 쉽습니다.

---

## 수업 10에서 데이터가 늘어난 이유

수업 1~9에서는 학습을 위해 최소한의 sample data를 사용했습니다.

수업 10에서는 실제 운영이 지속된 상황을 작게 재현하기 위해 `order-service` 데이터만 확장합니다.

```text
order-service Runbook
기존 4개
→ 12개

order-service recent incidents
기존 2건
→ 10건
```

중요한 것은 문자열을 일부러 길게 만든 것이 아닙니다.

각 Runbook과 History는 기존과 같은 **작고 의미 있는 Context 단위**를 유지합니다.

```text
긴 문자열 몇 개를 만든다  X
작은 Context 후보가 운영 중 축적된다  O
```

비용 문제의 핵심은 데이터 수 자체가 아닙니다.

```text
후보 3개
→ 3개 모두 전달

후보 10개
→ 10개 모두 전달

후보 100개
→ 100개 모두 전달
```

처럼 **검색 후보 수와 LLM 전달량이 함께 무제한으로 증가하는 구조**가 문제입니다.

---

## 비용 통제 전과 후

### Before

```text
관련 Runbook 검색
→ 정렬
→ 모두 전달

유사 History 검색
→ 정렬
→ 모두 전달

외부 Incident 입력
→ 별도 크기 제한 없이 Prompt 조립

Analysis LLM output
→ 그대로 Action Prompt에 재주입
```

### After

```text
Runbook
→ 관련 후보 검색
→ priority / updatedAt 정렬
→ deduplicationKey 중복 제거
→ severity별 Top N
→ Prompt
```

```text
History
→ 유사 후보 검색
→ similarity / occurredAt 정렬
→ severity별 Top N
→ Prompt
```

```text
외부 Incident 입력
→ field별 hard character limit
→ Prompt
```

```text
Analysis LLM output
→ field별 hard character limit
→ Action Prompt
```

```text
Final Prompt
→ 전체 문자열 임의 절단 안 함
→ safety budget 검증
```

---

## Context 성격에 따른 통제 방식

모든 Context를 같은 방법으로 줄이지 않습니다.

| Context                             | 성격                    | 수업 10 통제 방법                     |
| ----------------------------------- | --------------------- | ------------------------------- |
| Runbook                             | 내부에서 관리되는 작은 지식 단위    | deduplication + Top N           |
| History                             | 내부에서 관리되는 짧은 과거 장애 요약 | similarity Top N                |
| Escalation policy                   | 작고 구조화된 내부 정책         | 현재는 그대로 사용                      |
| Incident title / summary / symptoms | 외부 입력                 | hard character limit            |
| Analysis → Action 재주입               | LLM이 생성한 비결정적 출력      | field별 hard character limit     |
| 최종 Prompt                           | 고정 instructions 포함    | truncate하지 않고 safety validation |

핵심 구분은 다음과 같습니다.

```text
선택 가능한 내부 Context
→ selection budget

크기를 신뢰하기 어려운 입력 / LLM output
→ size budget
```

> **Context를 줄인다고 해서 모든 문자열을 같은 방식으로 자르는 것은 아닙니다.**

---

## Runbook Context Budget

수업 10의 첫 번째 정책값은 다음과 같습니다.

| Severity | 최대 Runbook |
| -------- | ---------: |
| LOW      |          1 |
| MEDIUM   |          2 |
| HIGH     |          4 |
| CRITICAL |          5 |

이 숫자는 운영 환경의 정답이 아닙니다.

> **중요한 것은 몇 개가 정답인가가 아니라 상한이 존재하는가입니다.**

실제 서비스에서는 input token과 응답 품질을 관찰하면서 조정해야 합니다.

대표 구현은 `RunbookContextBudget`입니다.

```java
public record RunbookContextBudget(
        int maxEntries
) {

    public static RunbookContextBudget from(
            IncidentSeverity severity
    ) {
        return switch (severity) {
            case LOW -> new RunbookContextBudget(1);
            case MEDIUM -> new RunbookContextBudget(2);
            case HIGH -> new RunbookContextBudget(4);
            case CRITICAL -> new RunbookContextBudget(5);
        };
    }
}
```

---

## Runbook deduplication

확장된 Runbook에는 같은 문제군을 표현하는 `deduplicationKey`가 있습니다.

예:

```text
ORD-DB-POOL-001
→ database-connectivity

ORD-DB-CONNECTION-004
→ database-connectivity

ORD-DB-SLOW-TX-005
→ database-connectivity

ORD-DB-FAILOVER-008
→ database-connectivity
```

수업 10에서는 이 값을 실제 Context 선택에 사용합니다.

```text
priority / updatedAt 정렬
  ↓
deduplicationKey 중복 제거
  ↓
severity별 Top N
```

순서가 중요합니다.

```text
Top N 먼저
→ 상위 결과가 같은 문제군에 몰릴 수 있음

dedup 먼저
→ 서로 다른 문제군을 확보
→ 그 뒤 Top N
```

`LocalRunbookKnowledgeAdapter`의 핵심 부분은 다음과 같습니다.

```java
return runbookCatalog
        .sortByPriorityAndRecency(candidates)
        .stream()
        .collect(
                Collectors.toMap(
                        this::deduplicationKey,
                        Function.identity(),
                        (existing, incoming) -> existing,
                        LinkedHashMap::new
                )
        )
        .values()
        .stream()
        .limit(budget.maxEntries())
        .map(RunbookEntry::content)
        .filter(content ->
                content != null
                        && !content.isBlank()
        )
        .toList();
```

동작 의미:

```text
this::deduplicationKey
→ Map key

Function.identity()
→ RunbookEntry 자체를 value로 사용

(existing, incoming) -> existing
→ 같은 key가 다시 나오면 먼저 들어온 Runbook 유지

LinkedHashMap::new
→ 기존 정렬 순서 유지
```

앞에서 이미 priority / updatedAt으로 정렬했기 때문에 같은 문제군에서는 우선순위가 높은 Runbook이 남습니다.

---

## History Context Budget

History는 기존 similarity scoring과 정렬 방식을 유지합니다.

수정하는 핵심은 **최종 반환 개수에 상한을 두는 것**입니다.

| Severity | 최대 History |
| -------- | ---------: |
| LOW      |          0 |
| MEDIUM   |          1 |
| HIGH     |          3 |
| CRITICAL |          4 |

대표 구현은 `HistoryContextBudget`입니다.

```java
public record HistoryContextBudget(
        int maxEntries
) {

    public static HistoryContextBudget from(
            IncidentSeverity severity
    ) {
        return switch (severity) {
            case LOW -> new HistoryContextBudget(0);
            case MEDIUM -> new HistoryContextBudget(1);
            case HIGH -> new HistoryContextBudget(3);
            case CRITICAL -> new HistoryContextBudget(4);
        };
    }
}
```

History Adapter의 흐름은 다음과 같습니다.

```text
같은 서비스의 History
→ similarity score 계산
→ score > 0 필터
→ score 내림차순
→ 같은 score면 최신 이력 우선
→ severity별 Top N
```

중요한 점은 다음과 같습니다.

> **검색 알고리즘을 더 정교하게 만드는 것과 검색 결과 중 얼마까지 LLM에 전달할지를 결정하는 것은 별개의 문제입니다.**

---

## severity를 Adapter까지 전달하는 이유

Runbook과 History의 최대 개수는 Incident severity에 따라 달라집니다.

따라서 Port에 severity를 추가합니다.

```text
IncidentResponseWorkflow
→ incident.severity() 전달

RunbookContextBudget / HistoryContextBudget
→ 정책값 결정

Adapter
→ 실제 선택량 적용
```

Workflow가 HIGH는 4개, CRITICAL은 5개 같은 숫자를 직접 알지는 않습니다.

책임은 다음처럼 나눕니다.

```text
Workflow
→ severity 전달

Budget object
→ 정책값 결정

Adapter
→ Context 선택 정책 적용
```

---

## 외부 Incident 입력 Size Budget

Runbook/History와 외부 입력은 성격이 다릅니다.

```text
Runbook / History
→ 내부에서 관리
→ 이미 작은 Context 단위
→ selection budget

Incident Request
→ 외부 입력
→ 예상보다 길 수 있음
→ size budget
```

수업 10에서는 다음과 같은 외부 입력에 문자 수 상한을 적용합니다.

```text
sourceSystem
title
summary
symptoms
```

대표적으로:

```java
PromptBudgetUtils.truncate(
        incident.summary(),
        budget.maxSummaryCharacters()
)
```

처럼 처리합니다.

이 hard limit은 정확한 token budget 계산이 아니라 **예상보다 긴 외부 입력이 Prompt를 무제한으로 키우지 않도록 하는 안전 상한**입니다.

---

## Analysis Result → Action Prompt 재주입

첫 번째 LLM의 output은 두 번째 LLM 입장에서는 다시 input입니다.

```text
Analysis LLM
  ↓
situationSummary
likelyCauses
impactAssessment
confidenceNote
  ↓
Action Prompt
  ↓
Action LLM
```

따라서 Analysis 결과가 길어질수록 Action Prompt의 input도 커질 수 있습니다.

수업 10에서는 네 field를 제거하지 않고 그대로 유지하되, 각각의 재주입 크기에 상한을 둡니다.

```text
situationSummary
→ 최대 800자

likelyCauses
→ 최대 900자

impactAssessment
→ 최대 600자

confidenceNote
→ 최대 400자
```

대표 구현:

```java
PromptBudgetUtils.truncate(
        output.situationSummary(),
        budget.maxSituationSummaryCharacters()
)
```

> **여러 번 LLM을 호출하는 애플리케이션에서는 이전 LLM의 output도 다음 호출에서는 다시 입력 비용이 됩니다.**

---

## `PromptBudgetUtils`

`PromptBudgetUtils`는 두 가지 역할을 합니다.

```text
truncate()
→ 크기를 신뢰하기 어려운 개별 문자열 제한

requireWithinBudget()
→ 완성된 Prompt 전체는 자르지 않고 안전 상한만 검증
```

`truncate()`는 외부 입력과 LLM output에 사용합니다.

Runbook이나 History처럼 이미 selection budget이 적용된 내부 Context를 무조건 자르는 용도로 사용하지 않습니다.

---

## 왜 final Prompt 전체를 truncate하지 않는가

다음과 같은 방식은 사용하지 않습니다.

```java
return PromptBudgetUtils.truncate(
        prompt,
        MAX_PROMPT_CHARACTERS
);
```

Prompt 끝에는 모델이 따라야 하는 고정 instruction이 포함됩니다.

```text
Return fields
JSON only instruction
```

최종 Prompt 전체를 앞에서 N자로 잘라버리면 Context뿐 아니라 이 고정 contract까지 손상될 수 있습니다.

따라서 다음 순서로 처리합니다.

```text
길어질 수 있는 가변 입력 먼저 제한
  ↓
Prompt 조립
  ↓
고정 JSON contract 보존
  ↓
최종 Prompt가 safety budget 안인지 검증
```

> **완성된 Prompt 전체를 마지막에 잘라내지 않습니다. 길어질 수 있는 입력을 먼저 제한하고, 최종 Prompt는 정해둔 크기를 넘지 않는지만 확인합니다.**

---

## 핵심 코드 위치

수업 10에서 새로 추가하거나 중요하게 변경되는 파일은 아래와 같습니다.

| 파일                                                                 | 수업 10에서의 책임                                       |
| ------------------------------------------------------------------ | ------------------------------------------------- |
| `incident/application/cost/RunbookContextBudget.java`              | severity별 Runbook 최대 개수 정책                        |
| `incident/application/cost/HistoryContextBudget.java`              | severity별 History 최대 개수 정책                        |
| `incident/application/cost/IncidentPromptBudget.java`              | 외부 입력 / LLM 재주입 / final Prompt safety budget 관리   |
| `common/prompt/PromptBudgetUtils.java`                             | 문자열 hard limit과 final Prompt validation           |
| `incident/port/out/IncidentKnowledgePort.java`                     | severity를 Runbook Adapter에 전달                     |
| `incident/port/out/IncidentHistoryPort.java`                       | severity를 History Adapter에 전달                     |
| `incident/adapter/out/knowledge/LocalRunbookKnowledgeAdapter.java` | 정렬 → dedup → severity Top N 적용                    |
| `incident/adapter/out/history/LocalIncidentHistoryAdapter.java`    | 기존 similarity ranking → severity Top N 적용         |
| `incident/application/IncidentResponseWorkflow.java`               | 현재 severity를 두 Port까지 전달                          |
| `incident/adapter/out/ai/IncidentPromptFactory.java`               | Context별 size budget 적용 및 final Prompt validation |
| `resources/sample-data/runbooks/order-service.yaml`                | 축적된 Runbook 상황 재현                                 |
| `resources/sample-data/recent-incidents.json`                      | 축적된 History 상황 재현                                 |

---

## 개선 전 확인

비용 통제 적용 전에는 확장 dataset만 적용한 상태에서 STANDARD 요청을 실행합니다.

확인할 항목:

```text
route
→ STANDARD_ANALYSIS

Analysis generation
→ 실제 Prompt
→ input token

Action generation
→ 실제 Prompt
→ input token
```

확장 데이터 기준으로 관련 후보는 다음처럼 증가할 수 있습니다.

```text
matched Runbook
→ 12개

matched History
→ 10건
```

중요한 것은 `12`, `10`이라는 숫자가 아닙니다.

> **후보가 늘어날 때 LLM에 전달하는 양도 상한 없이 같이 늘어나는 구조인지 확인하는 것이 핵심입니다.**

---

## 개선 후 예상 Context

같은 HIGH STANDARD 요청에서 비용 통제를 적용하면 다음 구조가 됩니다.

```text
Runbook
12
  ↓ deduplication
6
  ↓ HIGH Top N
4
```

```text
History
10
  ↓ HIGH Top N
3
```

실제 token 사용량은 Java 문자 수 계산으로 추정하지 않고 Langfuse에서 다시 확인합니다.

---

## Langfuse Before / After 비교

수업 10에서 가장 중요한 검증은 **같은 STANDARD 요청을 개선 전후에 다시 실행하는 것**입니다.

```text
Before
같은 request
→ STANDARD_ANALYSIS
→ Analysis LLM
→ Action LLM

After
같은 request
→ STANDARD_ANALYSIS
→ Analysis LLM
→ Action LLM
```

변경된 것은 Context 전달 정책입니다.

Langfuse에서 다음을 비교합니다.

```text
Analysis input token
Action input token
Prompt 실제 내용
```

실제 실습에서는 동일 요청 기준으로 **input token이 45% 이상 감소한 결과**를 확인했습니다.

이 결과는 다음을 의미합니다.

```text
Context 선택량과 크기 통제
→ 실제 모델 입력량 감소
```

하지만 다음처럼 해석하지 않습니다.

```text
input token 45% 감소
=
전체 비용 45% 절감
```

실제 비용은 모델 가격 정책을 적용해야 합니다.

```text
cost
=
input token × input price
+
output token × output price
```

따라서 정확한 설명은 다음과 같습니다.

> **같은 모델과 가격 체계를 사용할 때 input token 감소는 입력 비용을 낮추는 방향의 통제입니다.**

---

## latency를 성공 기준으로 두지 않는 이유

Prompt가 작아지면 처리량 감소를 기대할 수 있지만 LLM latency는 다음과 같은 영향을 받습니다.

```text
provider 부하
network
model scheduling
output 길이
일시적인 서버 상태
```

따라서 한 번의 실행에서 latency가 줄었다고 해서 성능이 몇 퍼센트 개선되었다고 단정하지 않습니다.

수업 10에서 직접 통제한 것은 입력 Context이므로 가장 직접적인 성공 지표는 **input token 변화**입니다.

---

## 최소 응답 품질 확인

수업 10은 품질 통제 수업은 아닙니다.

하지만 token을 줄인 뒤 응답이 완전히 무의미해졌다면 좋은 비용 통제라고 할 수 없습니다.

최소한 다음을 확인합니다.

```text
JSON mapping이 기본적으로 정상 동작하는가?
Incident와 관련된 상황 요약이 유지되는가?
주요 원인 후보가 완전히 사라지지 않았는가?
추천 액션이 Incident와 무관하지 않은가?
```

출력 계약과 의미 품질의 구체적인 검증은 다음 품질 통제 수업에서 다룹니다.

---

## 다른 AI 애플리케이션에서 비용 통제할 때 확인할 것

수업 10의 `Top 4`, `Top 3` 숫자를 그대로 복사하는 것이 목적이 아닙니다.

다른 시스템에서는 다음 순서로 접근하는 것이 중요합니다.

### 1) Context 증가 지점을 찾습니다.

```text
RAG 검색 결과
Runbook
과거 이력
사용자 입력
정책 문서
이전 LLM output
Tool 결과
대화 History
```

중 무엇이 Prompt를 키우는지 먼저 관찰합니다.

### 2) Context 단위를 확인합니다.

```text
긴 문서 전체를 넣고 있는가?
이미 작은 의미 단위로 준비되어 있는가?
```

### 3) 선택 정책을 만듭니다.

```text
관련도
우선순위
최신성
중복
Top N
```

업무에 필요한 기준을 선택합니다.

### 4) 통제되지 않은 입력에는 size limit을 둡니다.

```text
사용자 입력
외부 시스템 입력
이전 LLM output
```

처럼 크기를 신뢰하기 어려운 값에 명시적인 상한을 둡니다.

### 5) 같은 조건으로 다시 측정합니다.

```text
Prompt
input token
기본 응답 품질
```

을 다시 확인합니다.

> **비용 통제는 한 번 숫자를 줄이고 끝나는 것이 아니라 관찰 → 정책 적용 → 재관찰을 반복하는 과정입니다.**

---

## 더 정교한 Context 최적화 방법

수업 10에서는 구현하지 않지만 실제 시스템에서는 다음 방법을 검토할 수 있습니다.

### Tokenizer 기반 정확한 token budget

모델 tokenizer를 사용하면 문자 수가 아니라 실제 token 기준으로 더 정확하게 budget을 관리할 수 있습니다.

```text
장점
→ 실제 모델 token 기준의 정확한 상한

한계
→ 무엇이 중요한지는 결정하지 않음
```

### Embedding 기반 의미 중복 제거

표현은 달라도 의미적으로 비슷한 Context를 찾을 수 있습니다.

필요한 요소:

```text
embedding 생성
embedding 저장
similarity 계산
threshold 정책
```

현재 sample에서는 사람이 관리하는 `deduplicationKey`가 있기 때문에 더 단순하고 결정론적인 방식을 사용합니다.

### Vector similarity 기반 Context 선택

긴 원본 문서라면 전체를 Prompt에 넣지 않고 현재 요청과 관련 있는 chunk만 검색해 사용할 수 있습니다.

```text
긴 원본 문서
→ ingestion / chunking
→ vector search
→ 관련 chunk 선택
→ Prompt
```

현재 Runbook sample은 이미 작은 의미 단위로 준비되어 있으므로 이번 수업에서는 사용하지 않습니다.

### LLM summarization

긴 Context를 별도의 LLM으로 먼저 요약할 수도 있습니다.

```text
원본 Context
→ Summarization LLM
→ 압축 Context
→ Analysis LLM
```

하지만 다음 비용이 추가됩니다.

```text
추가 input/output token
추가 latency
추가 failure point
추가 요약 품질 검증
```

따라서 더 정교한 방법은 무조건 추가하는 것이 아니라 **현재 Context 구조와 비용 문제에 필요한 만큼만 도입**합니다.

---

## 현재 확인하는 범위

수업 10에서 확인하는 범위는 다음과 같습니다.

```text
- 운영 데이터 축적에 따른 Context 증가 문제
- 동일 STANDARD 요청 Before / After 비교
- Runbook deduplication
- Runbook severity Top N
- History similarity Top N
- Incident 외부 입력 hard character limit
- Analysis output field별 재주입 상한
- final Prompt safety validation
- Langfuse Analysis input token 비교
- Langfuse Action input token 비교
- 기본 응답 사용 가능성 확인
```

이번 수업에서 다루지 않는 범위는 다음과 같습니다.

```text
- Tokenizer를 직접 사용한 정확한 token budget
- Provider token counting API
- Embedding 기반 semantic deduplication 구현
- Vector similarity 기반 context compression 구현
- LLM summarization 구현
- 검색 알고리즘 전면 개선
- Vector DB 도입
- Reranker
- output contract validation
- retry / fallback
- Evaluator–Optimizer
- 위험 추천 분류
- Tool 실행
- 관리자 승인 / 실제 복구 workflow
```

---

## 정상 흐름 확인

애플리케이션 실행 후 아래 항목을 순서대로 확인합니다.

1. 애플리케이션이 정상 기동되는지 확인합니다.
2. Langfuse exporter 관련 오류가 없는지 콘솔에서 확인합니다.
3. 확장된 `order-service` Runbook과 History 데이터를 확인합니다.
4. 비용 통제 전 동일 STANDARD 요청을 실행합니다.
5. Langfuse에서 `STANDARD_ANALYSIS` route가 선택되었는지 확인합니다.
6. Analysis generation의 Prompt와 input token을 확인합니다.
7. Action generation의 Prompt와 input token을 확인합니다.
8. 비용 통제 코드에서 Runbook dedup + Top N이 적용되는지 확인합니다.
9. History Top N이 적용되는지 확인합니다.
10. 외부 Incident field에 hard limit이 적용되는지 확인합니다.
11. Analysis output 재주입 field에 hard limit이 적용되는지 확인합니다.
12. final Prompt 전체는 truncate하지 않고 safety validation만 수행하는지 확인합니다.
13. 같은 STANDARD 요청을 다시 실행합니다.
14. route와 두 LLM 호출이 동일하게 유지되는지 확인합니다.
15. Langfuse에서 개선 후 Analysis / Action input token을 확인합니다.
16. Before / After input token을 비교합니다.
17. 기본 응답이 Incident와 무관하게 붕괴하지 않았는지 확인합니다.


---

## 다음 수업과의 연결

```text
수업 9
→ 어떤 AI 경로를 실행할 것인가?
→ Flow Control

수업 10
→ 그 경로 안에서 얼마나 많은 Context를 허용할 것인가?
→ Cost Control

수업 11
→ LLM이 생성한 출력이 애플리케이션이 요구하는 기본 계약을 지키는가?
→ Quality Control
```

수업 10에서 입력 Context의 양을 통제했다고 해서 LLM 출력의 구조와 내용이 자동으로 보장되는 것은 아닙니다.

다음 수업에서는 JSON mapping failure, 필수값, 목록 개수, 중복, priority 범위 같은 **출력의 기본 계약**을 통제합니다.

---

## 마무리

이 코드 스냅샷의 목적은 단순히 `Top 4`, `Top 3` 숫자를 적용하는 것이 아닙니다.

기존에는:

```text
관련 Context 증가
→ 전달량도 함께 증가
→ Prompt 증가
→ input token 증가 가능
```

했다면 수업 10 이후에는:

```text
관련 Context 검색
→ 우선순위 / 관련성 유지
→ 중복 제거
→ 선택량 상한
→ 크기 상한
→ Prompt 조립
→ Langfuse에서 재관찰
```

하는 구조로 바뀝니다.

실제 동일 STANDARD 요청에서 input token이 45% 이상 감소한 결과를 확인했지만, 이 숫자 자체가 수업의 최종 목표는 아닙니다.

수업 10에서 가져가야 할 최종 메시지는 다음과 같습니다.

> **비용 통제는 token 숫자를 보는 것으로 끝나지 않습니다.**
>
> **애플리케이션이 어떤 Context를 선택하고, 중복을 제거하고, 얼마까지 전달할지 명시적으로 결정해야 합니다.**
>
> **그리고 그 정책이 실제 input token과 기본 응답 품질에 어떤 영향을 주는지 다시 관찰해야 합니다.**

가장 짧게 정리하면:

> **관련 데이터는 찾고, 전달량은 통제합니다.**
