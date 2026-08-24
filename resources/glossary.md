# 용어집

> 목적: 수업에서 등장하는 어려운 용어를
> 짧고 쉽게 찾아볼 수 있도록 정리한다.
>
> 이 문서는 구현 방법을 설명하는 문서가 아니다.
> 코드 리뷰 중 용어가 낯설 때 참고하는 보조 자료다.

---

## 1. 통제 4축

| 용어 | 쉬운 뜻 |
|---|---|
| 비용(Cost) | AI 요청 하나를 처리하는 데 토큰, 모델 호출, 시간 같은 자원이 얼마나 드는지 |
| 품질(Quality) | AI 응답이 형식과 내용 면에서 믿을 만한지 |
| 리스크(Risk) | 위험한 추천이나 실행을 막고, 승인·권한·정책을 적용하는지 |
| 흐름(Flow) | 요청이 어떤 순서와 경로로 처리되는지, 그 경로를 확인하고 제어할 수 있는지 |
| 마킹 포인트(Marking Point) | 지금 코드에서 통제가 부족한 문제 위치를 표시한 주석 지점 |
| baseline | 개선하기 전의 기준 코드. 현재 동작은 하지만 운영 통제는 아직 부족한 상태 |

---

## 2. Prompt·비용 용어

| 용어 | 쉬운 뜻 |
|---|---|
| LLM | 사람의 언어를 이해하고 생성하는 AI 모델 |
| Prompt | LLM에게 보내는 요청문. 지시문과 참고 자료를 함께 담는다 |
| PromptFactory | Prompt를 조립하는 클래스 |
| 컨텍스트(Context) | LLM이 판단할 때 함께 참고하도록 Prompt에 넣는 정보. 이 앱에서는 런북, 과거 장애 이력, 정책 등이 해당한다 |
| 런북(Runbook) | 장애가 발생했을 때 확인할 내용과 대응 절차를 적어 둔 운영 문서 |
| 런북 스니펫(Runbook Snippet) | 런북 전체가 아니라, 현재 장애와 관련된 일부 본문 |
| 유사 장애 이력(Similar Incident) | 현재 장애와 비슷하게 발생했던 과거 장애 기록 |
| resolutionSummary | 과거 장애를 어떻게 해결했는지 요약한 텍스트 |
| input token | LLM에 보내는 Prompt 텍스트를 모델이 처리하는 단위 |
| output token | LLM이 생성해서 돌려주는 응답 텍스트를 모델이 처리하는 단위 |
| 토큰 비용 | LLM에 넣고 받는 토큰 양에 따라 늘어나는 사용 비용 |
| Prompt 길이 | LLM에 보내는 전체 텍스트의 양 |
| 토큰 예산(Token Budget) | 한 번의 LLM 호출에서 Prompt에 넣을 수 있는 최대 토큰 양을 미리 정한 기준 |
| Context Budget | 런북·이력 같은 참고 정보를 Prompt에 얼마나 넣을지 정한 한도 |
| top N | 관련 결과를 모두 쓰지 않고, 우선순위가 높은 앞쪽 N개만 선택하는 방식 |
| 트렁케이션(Truncation) | 너무 긴 텍스트를 정해 둔 길이까지만 잘라내는 것 |
| 중복 제거(Deduplication) | 비슷하거나 같은 런북·이력·액션을 하나만 남기는 것 |
| 재주입 | 이전 LLM 호출의 결과를 다음 LLM 호출 Prompt에 다시 넣는 것 |
| adapter | 파일, DB, 외부 API 같은 데이터 원본에서 데이터를 가져와 애플리케이션에 전달하는 연결 부품 |
| local sample-data | 실제 운영 시스템 대신 실습용으로 프로젝트 내부에 준비한 로컬 데이터 |
| 캡핑(capping) | LLM에 전달되는 전체 정보량에 상한을 두는 통제 방식 |

---

## 3. 품질·출력 용어

| 용어 | 쉬운 뜻 |
|---|---|
| JSON | 필드 이름과 값을 `{ }` 구조로 표현하는 데이터 형식 |
| JSON only | LLM에게 설명 문장 없이 JSON 형식만 반환하라고 지시하는 것 |
| Markdown 코드 펜스 | Markdown에서 코드를 표시하려고 쓰는 ```` ```json ```` 같은 표시 |
| DTO | 계층이나 시스템 사이에서 데이터를 전달하기 위한 객체 |
| record | Java에서 데이터를 담는 간단한 객체를 만들 때 사용하는 문법 |
| 타입(Type) | 값이 String인지, 숫자인지, 목록인지처럼 데이터의 형태를 나타내는 약속 |
| 타입 안전성 | 코드가 기대하지 않은 데이터 형태가 들어오는 일을 줄이는 성질 |
| 타입 구조 | 어떤 필드가 어떤 타입으로 구성되는지에 대한 모양 |
| 매핑(Mapping) | LLM이 반환한 JSON 텍스트를 Java DTO 객체로 바꾸는 과정 |
| mapping failure | JSON 형식이 틀렸거나 필드 타입이 맞지 않아 DTO로 바꾸지 못하는 실패 |
| 출력 계약(Output Contract) | AI 응답이 최소한 지켜야 하는 규칙. 예: 필수 값이 비어 있지 않음, priority가 허용 범위 안임 |
| contract failure | DTO 변환은 성공했지만, 빈 값·누락 필드·잘못된 priority처럼 출력 계약을 어긴 실패 |
| 검증(Validation) | 값이 필수인지, 범위 안인지, 비어 있지 않은지 확인하는 과정 |
| JSON Schema 검증 | JSON에 필요한 필드와 타입이 있는지 정해 둔 구조 규칙으로 검사하는 것 |
| 재시도(Retry) | LLM 응답이 잘못되었을 때 다시 요청하는 처리 |
| fallback | 재시도도 실패했을 때 예외를 그대로 키우지 않고 대신 반환하는 안전한 결과 또는 대체 처리 |
| Evaluator-Optimizer | AI 결과를 한 번 평가하고, 부족하면 개선 요청을 통해 더 나은 결과를 만들려는 패턴 |
| 의미적 품질 | 형식만 맞는지를 넘어, 원인과 액션이 실제 장애 근거에 맞고 충분히 구체적인지 |

---

## 4. 리스크·정책 용어

| 용어 | 쉬운 뜻 |
|---|---|
| 추천 액션(Recommended Action) | AI가 운영자에게 제안하는 다음 대응 행동 |
| actionTitle | 추천 액션의 짧은 제목 |
| rationale | 왜 이 액션을 추천했는지 설명하는 근거 |
| ownerSuggestion | 이 액션을 담당하면 좋을 팀 또는 역할 제안 |
| 위험 키워드 필터 | restart, rollback, scale처럼 위험할 수 있는 행동 단어를 찾아 별도 처리하는 규칙 |
| 승인(Approval) | 위험한 행동을 진행하기 전에 권한 있는 사람이 확인하고 허용하는 절차 |
| approvalRequired | 이 추천 또는 행동에 사람 승인이 필요한지를 나타내는 값 |
| 정책(Policy) | 어떤 상황에서 누가, 언제, 무엇을 해야 하는지 정한 운영 규칙 |
| 정책 문자열 | 정책 내용을 사람이 읽을 수 있도록 텍스트로 만든 것 |
| policy enforcement | 정책을 설명으로만 보여 주지 않고, 실제 허용·차단·승인·알림 동작에 적용하는 것 |
| sourceSystem | 장애가 발생한 서비스 이름. 예: payment-service, order-service |
| severity | 장애 심각도. 예: LOW, MEDIUM, HIGH, CRITICAL |
| requestedSeverity | 요청에 처음 들어온 심각도 |
| minimumSeverity | 특정 서비스는 최소 이 심각도 이상으로 처리해야 한다는 정책 값 |
| effectiveSeverity | 요청 심각도와 최소 심각도 정책을 비교한 뒤, 실제 대응에 사용할 최종 심각도 |
| businessCriticality | 서비스가 사업적으로 얼마나 중요한지 나타내는 등급 |
| 에스컬레이션(Escalation) | 장애가 정해진 시간 안에 대응되지 않거나 더 높은 전문성·권한이 필요할 때, 적절한 사람과 팀에게 대응 책임을 단계적으로 올리는 절차 |
| escalation policy | 누가 먼저 대응하고, 응답이 없으면 누구에게 언제 넘길지 정한 규칙 |
| war room | 심각한 장애 상황에서 관련 담당자가 빠르게 모여 공동 대응하는 비상 협업 공간 |
| notifyChannels | 장애 상황을 알릴 채널. 예: Slack, PagerDuty |
| Tool 실행 | AI 추천을 실제 시스템 변경 행동으로 수행하는 것 |
| Tool gate | 실제 Tool 실행 전에 권한, 환경, 승인 조건을 검사해 허용 또는 차단하는 장치 |

---

## 5. 흐름·관찰 용어

| 용어 | 쉬운 뜻 |
|---|---|
| 요청 파이프라인(Request Pipeline) | HTTP 요청이 들어와 응답이 나갈 때까지 거치는 전체 처리 순서 |
| 선형 흐름(Linear Flow) | 중간 선택 없이 모든 요청이 같은 순서로 끝까지 처리되는 흐름 |
| 분기(Branching) | 조건에 따라 코드 흐름이 여러 경로로 나뉘는 것 |
| 라우팅(Routing) | 요청의 특성을 보고 적절한 처리 경로를 고르는 것 |
| route | 라우팅 결과로 선택된 처리 경로 이름 |
| handler | 특정 route를 실제로 처리하는 담당 클래스 또는 메서드 |
| `QUICK_TRIAGE` | 낮은 심각도 요청을 빠르게 초기 분류하고 기본 상태를 확인하는 경로 |
| `STANDARD_ANALYSIS` | 런북·과거 이력·정책을 참고해 일반적인 분석과 액션 추천을 수행하는 경로 |
| `CRITICAL_ESCALATION` | 심각한 장애를 긴급 대응과 에스컬레이션 중심으로 처리하는 경로 |
| 관찰 가능성(Observability) | 시스템 내부에서 실제로 무슨 일이 일어났는지 로그·메트릭·trace로 확인할 수 있는 성질 |
| trace | 요청 한 건이 시스템에서 지나간 전체 경로 |
| traceId | 같은 요청에서 발생한 로그와 trace를 한 묶음으로 찾기 위한 식별자 |
| span | trace 안에서 기록하는 작업 한 구간. 예: 런북 조회, 분석 LLM 호출 |
| 서비스 span(Service Span) | Service 메서드 하나의 시작·종료 시간, 성공·실패를 기록한 span |
| parent-child trace 연결 | 큰 작업의 trace 아래에 하위 작업 span을 연결해, 작업 관계를 트리처럼 보여 주는 구조 |
| event | span 진행 중 특정 순간에 남기는 이정표 기록. 예: `knowledge-loaded`, `prompt-built` |
| observation event | 관찰 목적의 event. 단계가 완료됐거나 특정 상태가 발생했음을 trace에 남긴다 |
| latency | 요청 또는 특정 단계가 완료되기까지 걸린 시간 |
| error span | span 실행 중 오류가 발생했다는 정보가 기록된 span |
| Micrometer Tracing | Spring 애플리케이션에서 trace와 span을 다루도록 돕는 관찰 라이브러리 |
| OpenTelemetry(OTEL) | trace, span, 로그, 메트릭 같은 관찰 데이터를 표준 방식으로 수집·전달하기 위한 기술 표준 |
| Spring AI Observation | Spring AI의 LLM 호출, Prompt, Tool 같은 작업을 관찰 데이터로 남기기 위한 기능 |
| Langfuse trace | Langfuse 화면에서 확인하는 AI 요청 단위의 실행 기록 |
| ChatClient | 애플리케이션 코드가 LLM에 Prompt를 보내고 응답을 받는 Spring AI 클라이언트 |
| RAG | 외부 문서나 지식을 찾아 Prompt에 넣고 LLM이 참고하게 하는 방식 |
| MDC | Mapped Diagnostic Context의 약자. 로그에 traceId 같은 공통 정보를 붙이기 위해 사용하는 임시 key-value 문맥 |
| MDC 로그 | MDC에 저장한 traceId, incidentId 등을 로그 출력 시 함께 표시한 로그 |
| 로그 상관관계(Log Correlation) | 같은 traceId를 기준으로 한 요청에서 나온 여러 로그를 묶어 찾는 것 |
| 응답 헤더 전파 | traceId를 HTTP 응답 헤더에 넣어 호출한 클라이언트도 요청 식별자를 알 수 있게 하는 것 |
| 죽은 관찰 지점 | 관찰용 메서드나 코드가 존재하지만 실제 요청 흐름에서 호출되지 않아 기록을 남기지 못하는 상태 |

---

## 6. 빠른 구분표

```text
MDC
  → 로그에 traceId를 붙여 같은 요청의 로그를 찾기 쉽게 한다.

trace
  → 요청 한 건의 전체 처리 경로를 보여 준다.

span
  → trace 안에서 ‘런북 조회’, ‘LLM 분석’ 같은 작업 한 구간을 보여 준다.

event
  → span 안에서 ‘런북 조회 완료’, ‘Prompt 생성 완료’ 같은 이정표를 남긴다.

routing
  → 이 요청을 어느 처리 경로로 보낼지 결정한다.

fallback
  → 정상 처리와 재시도까지 실패했을 때 사용할 안전한 대체 처리다.

policy
  → 어떤 조건에서 누가 무엇을 할지 정한 운영 규칙이다.

policy enforcement
  → 정책을 문장으로만 보여 주지 않고 실제 허용·차단·승인·알림에 적용하는 것이다.
```

---

## 7. 수업 6에서 기억할 최소 용어

```text
컨텍스트
  → LLM에게 함께 주는 참고 정보

출력 계약
  → AI 응답이 최소한 지켜야 할 규칙

에스컬레이션
  → 더 높은 전문성·권한이 필요한 장애를 적절한 사람과 팀으로 단계적으로 올리는 절차

trace
  → 요청 한 건의 전체 경로

span
  → 그 경로 안의 작업 한 구간

event
  → 작업 중간의 이정표

MDC
  → 로그에 공통 식별자를 붙이는 임시 문맥

routing
  → 요청에 맞는 처리 경로를 고르는 것
```