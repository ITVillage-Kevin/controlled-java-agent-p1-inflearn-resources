# 용어집 — Controlled Java AI Part 1

> **대상:** [Part 1] Spring AI 사례로 배우는 AI 애플리케이션 통제 기법: 비용·품질·리스크·흐름 통제  
> **목적:** 수업에서 만나는 기술 용어와 코드의 이름을 짧고 쉽게 찾아보는 참고 자료  
> **사용법:** 먼저 아래 **수업별 용어 안내**에서 해당 수업을 찾고, 각 주제별 표에서 뜻을 확인하세요. 구체적인 구현과 실행 방법은 각 수업의 `README.md`를 참고하세요.

**이 강의의 핵심:** AI가 항상 옳게 답하도록 만드는 것이 아니라, AI의 판단을 Java/Spring 애플리케이션에서 **관찰·제한·검증·정책 적용**할 수 있도록 통제 지점을 설계합니다.

> **범위 주의:** 이 강의는 AI가 장애 대응을 **추천**하는 Incident Responder를 다룹니다. 실제 서버 재시작·롤백·스케일 변경, Tool 실행, 사람의 승인 처리, 승인 후 작업 실행, 자동 복구는 구현하지 않습니다. `approvalRequired`는 실제 승인 절차가 아니라 **승인 필요 여부를 나타내는 정보**입니다.

---

## 1. 통제 4축과 공통 개념

| 용어 | 쉬운 뜻 |
|---|---|
| 흐름 통제(Flow Control) | 요청이 실제로 어디를 거쳐 처리되는지 관찰하고, 요청 특성에 맞는 경로를 선택하는 것 |
| 비용 통제(Cost Control) | 모델 호출과 Prompt에 포함할 정보의 양을 관리해 불필요한 토큰 사용을 줄이는 것 |
| 품질 통제(Quality Control) | AI 응답의 기본 형식·계약과 내용 품질을 검증하고, 필요한 범위에서 개선하는 것 |
| 리스크 통제(Risk Control) | AI가 추천한 대응 행동의 위험을 분류하고, Java 정책에 따라 최종 노출 결과를 조정하는 것 |
| 기준 구현(Baseline) | 정상적으로 동작하지만 관찰·비용·품질·리스크 통제가 아직 충분하지 않은 출발점 코드. 잘못 만든 코드라는 뜻은 아님 |
| 개선 전/후(Before/After) | 같은 문제와 요청을 기준으로 통제 추가 전후의 동작·출력을 비교하는 방식 |
| 통제 지점(Control Point) | 애플리케이션이 요청 흐름, 입력 크기, 출력 품질 또는 위험 추천에 규칙을 적용하는 위치 |
| 마킹 포인트(Marking Point) | 수업 6에서 앞으로 개선할 통제 문제를 코드 주석 등으로 표시한 위치 |
| 결정론적 규칙(Deterministic Rule) | 같은 입력과 조건에 대해 정해진 규칙대로 처리하는 Java 로직. 예: 허용 개수, 위험 분류 조건, 최대 수정 횟수 |
| 확률적 동작(Probabilistic Behavior) | 같은 질문에도 LLM의 응답이 달라질 수 있는 성질 |
| 책임 분리(Separation of Responsibilities) | AI가 생성·평가할 내용과 Java 애플리케이션이 보장할 정책·제약을 구분하는 설계 |
| 워크플로(Workflow) | 요청을 처리하기 위해 여러 작업과 서비스를 연결하는 흐름. 이 강의에서는 Java 코드가 처리 순서를 관리함 |

---

## 2. Incident Responder와 도메인 용어

| 용어 | 쉬운 뜻 |
|---|---|
| 장애(Incident) | 서비스에서 발생한 오류·지연·이상 상황을 나타내는 사건 |
| Incident Responder | Incident를 분석하고 대응 액션을 추천하는 강의 사례 애플리케이션 |
| `IncidentResponseWorkflow` | 요청 경로를 선택하고 분석·추천·정책 처리의 상위 흐름을 연결하는 클래스 |
| `IncidentAnalysisService` | Incident 정보와 참고 자료를 사용해 장애 분석을 생성하는 서비스 |
| `IncidentActionRecommendationService` | 분석 결과 등을 바탕으로 대응 액션을 추천하는 서비스 |
| `Incident` | 장애 정보를 담는 도메인 객체 |
| `sourceSystem` | 장애가 발생한 서비스나 시스템의 식별 이름. 예: `payment-service` |
| `symptoms` | 장애를 나타내는 관찰 증상 목록. 예: 응답 지연, 오류 증가 |
| `severity` | 장애의 심각도를 나타내는 값. **장애 심각도**와 개별 **추천 액션의 위험 수준**은 다름 |
| `requestedSeverity` | 외부 요청에서 전달된 최초 장애 심각도 |
| `minimumSeverity` | 서비스 정책에서 요구하는 최소 장애 심각도 |
| `effectiveSeverity` | 요청 심각도와 최소 심각도 정책을 적용한 후 실제 처리에 사용하는 심각도 |
| `businessCriticality` | 해당 서비스가 비즈니스에 얼마나 중요한지를 나타내는 정책 정보. Incident의 현재 심각도와는 구별됨 |
| 추천 액션(Recommended Action) | AI가 운영자에게 제안하는 대응 행동. 이 애플리케이션이 직접 실행하는 명령은 아님 |
| `actionTitle` | 추천 액션의 짧은 제목 |
| `rationale` | 해당 액션을 추천한 이유와 근거 |
| `ownerSuggestion` | 추천 액션을 담당할 만한 팀 또는 역할에 대한 제안 |
| 런북(Runbook) | 장애 확인 방법과 대응 절차를 기록한 운영 문서 |
| 런북 스니펫(Runbook Snippet) | 전체 런북 중 현재 Incident와 관련된 일부 내용 |
| 유사 장애 이력(Similar Incident) | 현재 Incident와 비슷한 과거 장애 기록 |
| `resolutionSummary` | 과거 장애를 어떻게 해결했는지 요약한 정보 |
| 어댑터(Adapter) | 로컬 샘플 데이터나 외부 데이터 원본을 애플리케이션의 인터페이스에 연결하는 구성요소 |
| 로컬 샘플 데이터(Local Sample Data) | 실습을 위해 프로젝트 내부에 준비한 Runbook·장애 이력·정책 데이터 |

---

## 3. LLM · Prompt · Context · 비용

| 용어 | 쉬운 뜻 |
|---|---|
| 대규모 언어 모델(Large Language Model, LLM) | 입력된 텍스트를 바탕으로 언어 응답을 생성하는 AI 모델 |
| 프롬프트(Prompt) | 모델에 전달하는 지시와 입력 내용. 필요한 경우 Incident 정보, Runbook, 이력, 정책 문맥을 포함함 |
| `ChatClient` | Spring AI에서 Prompt를 구성해 모델을 호출하고 응답을 처리하는 클라이언트 API |
| `IncidentPromptFactory` | 강의 코드에서 분석·추천 등에 필요한 Prompt를 조립하는 클래스 |
| 컨텍스트(Context) | LLM이 현재 요청을 처리할 때 참고하도록 제공하는 정보 |
| 검색 증강 생성(Retrieval-Augmented Generation, RAG) | 관련 문서·데이터를 찾아 LLM 입력에 포함해 답변에 활용하는 방식 |
| 입력 토큰(Input Token) | 모델이 입력을 처리할 때 계산되는 토큰 수. Prompt에 포함된 지시와 참고 정보 등이 영향을 줌 |
| 출력 토큰(Output Token) | 모델이 생성한 응답에 해당하는 토큰 수 |
| 토큰 사용량(Token Usage) | 입력·출력에 사용된 토큰의 수. 모델 호출 비용을 이해하는 주요 지표 |
| 모델 호출 비용(Model Cost) | 사용한 모델과 입력·출력 토큰 등의 과금 기준에 따라 발생하는 비용. 지연 시간과는 구별됨 |
| Prompt 크기 | 모델에 전달하는 전체 입력의 양. **문자 수와 토큰 수는 동일하지 않음** |
| 컨텍스트 예산(Context Budget) | Runbook·과거 이력 등 참고 정보를 얼마나 포함할지 정한 애플리케이션의 한도 |
| 토큰 예산(Token Budget) | 토큰 수를 기준으로 설정한 한도. **이 강의의 주요 비용 실습은 토큰을 정확히 세어 차단하기보다 데이터 개수·길이를 직접 제한**하는 방식임 |
| 상위 N개 선택(Top N) | 관련도가 높은 결과부터 정해진 N개만 사용하는 방식 |
| 길이 제한(Truncation) | 텍스트가 허용 길이를 넘으면 정해진 범위까지만 사용하는 처리 |
| 캡핑(Capping) | 데이터 개수·문자 길이·재주입 필드 등 입력량에 상한을 두는 방식 |
| 중복 제거(Deduplication) | 같은 항목이 반복해서 전달되지 않도록 중복 데이터를 제거하는 것 |
| `deduplicationKey` | 같은 의미의 항목을 하나로 묶거나 중복 여부를 판단하는 데 사용하는 키 |
| 재주입(Re-injection) | 앞선 LLM 호출에서 얻은 결과를 다음 LLM 호출의 입력에 다시 포함하는 것 |
| 컨텍스트 선택(Context Selection) | 검색 결과를 모두 넣지 않고 현재 요청에 필요한 자료만 고르는 과정 |
| 입력 토큰 감소율 | 개선 전후 동일 조건에서 Input Token이 얼마나 줄었는지 나타내는 비교 지표. 측정 결과는 요청·모델에 따라 달라질 수 있음 |

---

## 4. 출력 형식 · 기본 계약 · 품질 통제

| 용어 | 쉬운 뜻 |
|---|---|
| JSON(JavaScript Object Notation) | 필드 이름과 값을 구조화해 표현하는 데이터 형식 |
| JSON-only 지시 | 모델에 JSON 이외의 설명이나 코드 블록 표시 없이 응답하도록 요청하는 지시. **지시만으로 형식 준수가 보장되지는 않음** |
| Markdown 코드 펜스(Code Fence) | 코드 블록을 표시하는 ``` 같은 기호. JSON-only 출력에서는 이런 표시가 섞이지 않도록 검사함 |
| 데이터 전달 객체(Data Transfer Object, DTO) | 계층 간 데이터를 주고받기 위한 객체 |
| `record` | Java에서 데이터를 담는 불변 데이터 중심 객체를 간결하게 정의하는 문법 |
| 타입(Type) | 필드 값이 문자열·숫자·목록 등 어떤 종류인지 나타내는 정보 |
| 타입 구조(Typed Structure) | 출력 객체에 어떤 필드와 타입이 정의되어 있는지 나타내는 형태 |
| 매핑(Mapping) | LLM 응답 문자열을 Java 객체로 변환하는 처리 |
| 매핑 실패(Mapping Failure) | JSON 형식이나 타입 등이 맞지 않아 객체로 변환하지 못한 상태 |
| 출력 계약(Output Contract) | AI 결과가 충족해야 하는 애플리케이션의 기본 규칙. 예: 필수값, 목록 개수, 중복 금지, `priority` 범위 |
| 계약 위반(Contract Failure) | Java 객체로 변환은 되었지만 필수값·개수·범위 등 기본 규칙을 충족하지 못한 상태 |
| 검증(Validation) | 값과 구조가 미리 정한 조건을 만족하는지 확인하는 과정 |
| `JsonOnlyResponseValidator` | 응답 원문이 JSON-only 형식을 지키는지 확인하는 수업 11 구성요소 |
| `BeanOutputConverter` | Spring AI에서 모델 출력 텍스트를 지정한 Java 타입으로 변환하는 데 사용하는 구성요소 |
| `IncidentAiOutputConverter` | 강의에서 LLM 출력의 객체 변환을 담당하도록 분리한 구성요소 |
| `IncidentAiOutputValidator` | 변환된 출력 객체의 기본 계약을 검사하는 구성요소 |
| `ValidationResult` | 검증 성공 여부와 실패 이유 등을 표현하는 결과 |
| 재시도(Retry) | 출력 형식이나 기본 계약에 문제가 있을 때, 정해진 조건·횟수 안에서 다시 요청하는 처리 |
| 대체 처리(Fallback) | 정상 처리나 허용된 재시도가 실패했을 때 정해진 안전한 결과 또는 처리로 전환하는 것 |
| 의미적 품질(Semantic Quality) | 형식이 아니라 내용이 근거와 맞는지, 구체적인지, 과도한 확신이 없는지, 추천이 적절한지를 보는 기준 |
| 평가자–개선자(Evaluator–Optimizer) | 생성된 결과를 평가하고, 개선이 필요하면 수정한 뒤 다시 평가하는 패턴 |
| 평가자(Evaluator) | 현재 AI 결과가 내용 품질 기준을 충족하는지 판단하는 역할 |
| 개선자(Optimizer) | Evaluator의 피드백을 참고해 AI 결과를 개선하는 역할 |
| 재평가자(Re-Evaluator) | Optimizer가 수정한 결과를 다시 평가하는 단계. 별도의 새로운 패턴을 뜻하지 않음 |
| `IncidentQualityControlService` | Analysis/Action 서비스 내부에서 품질 평가·개선 흐름을 조정하는 서비스. **Workflow가 직접 호출하는 독립 단계는 아님** |
| `PASS` | Evaluator가 현재 결과를 품질 기준에 적합하다고 판단한 평가 결정 |
| `NEEDS_REVISION` | Evaluator가 현재 결과에 개선이 필요하다고 판단한 평가 결정 |
| `PASSED` | 최초 결과가 내용 품질 평가를 통과한 최종 품질 통제 상태 |
| `IMPROVED` | 한 번 수정한 결과가 기본 계약 검증과 재평가를 통과한 최종 상태 |
| `NOT_RESOLVED` | 허용된 개선 범위 안에서 품질 문제가 해결되지 않은 최종 상태 |
| 최대 1회 개선 | Optimizer의 수정 횟수를 한 번으로 제한하는 Java 정책. 계속 반복해서 LLM을 호출하지 않도록 함 |

> **구분:** `PASS` / `NEEDS_REVISION`은 **평가 결정**이고, `PASSED` / `IMPROVED` / `NOT_RESOLVED`는 **최종 품질 통제 상태**입니다. JSON/DTO 변환 성공만으로 내용 품질이 보장되지는 않습니다.

---

## 5. 위험 분류 · 정책 · Advisor

| 용어 | 쉬운 뜻 |
|---|---|
| 위험 수준(Risk Level) | **개별 추천 액션**이 얼마나 위험한지를 나타내는 분류. Incident의 `severity`와 다른 개념 |
| `riskLevel` | 추천 액션의 위험 분류 결과를 담는 필드 |
| `riskReason` | 해당 추천을 특정 위험 수준으로 분류한 이유 |
| `approvalRequired` | 추천 액션에 사람의 사전 승인이 필요하다는 **표시값**. 실제 승인 요청·처리 기능은 아님 |
| 위험 분류기(`ActionRiskClassifier`) | 추천 문구에 나타난 삭제·롤백·재시작·조회 등의 표현을 규칙으로 판별하는 클래스 |
| 위험 분류(`ActionRiskAssessment`) | 분류된 위험 수준과 판단 이유 등 추천 액션의 위험 평가 정보 |
| 위험 키워드 규칙 | `delete`, `rollback`, `restart` 등 위험을 암시하는 표현을 기준으로 분류하는 **강의용 결정론적 단순화**. 운영 시스템의 실제 안전성을 완전히 판단하는 기술은 아님 |
| 정책(Policy) | 애플리케이션이 상황별로 적용하는 명시적 규칙 |
| 정책 적용(Policy Enforcement) | 정책을 Prompt에 설명하는 데 그치지 않고 **Java 코드에서 실제 결과를 제한·제외·조정**하는 것 |
| `IncidentRiskPolicyService` | 추천 액션의 위험 분류 결과를 바탕으로 중복 제거·제외 등 최종 응답 정책을 적용하는 서비스 |
| 추천 액션 개수 0~3개 | LLM의 기본 출력 계약과 달리, **최종 위험 정책 적용 후에는 추천이 제거되어 3개보다 적거나 0개일 수도 있음** |
| 에스컬레이션(Escalation) | 장애의 심각도나 운영 정책에 따라 대응 책임·연락 대상을 상위 단계로 올리는 절차 |
| 에스컬레이션 정책(Escalation Policy) | 대응 담당·알림 대상·필요한 조치 등을 정한 운영 규칙 |
| `EscalationDecision` | 적용된 에스컬레이션 정책의 결과를 Java에서 구조화해 담는 객체 |
| `notifyChannels` | 정책에서 지정한 알림 대상 채널 정보. **이 값이 있다고 실제 알림을 전송하는 것은 아님** |
| `warRoomRequired` | 공동 대응 공간이 필요하다는 정책상 판단을 나타내는 값. 실제 공간 생성 기능은 아님 |
| 정책 문맥(Policy Context) | 이미 Java에서 결정한 정책을 LLM이 참고할 수 있도록 Prompt에 제공하는 정보 |
| Advisor | Spring AI `ChatClient` 호출 과정에서 요청·응답 처리에 공통 로직을 적용할 수 있는 확장 지점 |
| `IncidentPolicyAdvisor` | 이미 결정된 `EscalationDecision`을 읽어 관련 LLM 요청의 Prompt에 공통 정책 문맥을 추가하는 Advisor |
| `ChatClientRequest` Context | Advisor가 LLM 요청을 처리할 때 참조할 수 있는 요청별 부가 데이터 영역 |
| `AdvisorSpec` | `ChatClient` 호출에 Advisor 및 관련 Context를 설정하는 데 사용하는 구성 방식 |
| `defaultAdvisors` | `ChatClient`에 공통 Advisor를 기본 등록하는 설정 방식 |
| `request.mutate()` | 기존 요청 내용을 바탕으로 필요한 값을 바꾼 새 요청을 구성하는 방식 |
| `promptWithPolicy()` | 강의 코드에서 구조화된 정책 문맥을 LLM 요청에 전달하기 위해 사용하는 호출 경계의 구성 방식 |
| 공통 정책 적용 vs 정책 문맥 전달 | **Java 정책 적용**은 결과를 실제로 제한하는 일이고, **Advisor의 정책 문맥 전달**은 LLM에게 참고 정보를 제공하는 일. 서로 대체할 수 없음 |

> **범위 주의:** `approvalRequired`, `notifyChannels`, `warRoomRequired`는 외부 시스템이 활용할 수 있는 **정책 정보**입니다. 이 강의의 Incident Responder가 실제 승인·알림·복구를 수행한다는 뜻은 아닙니다.

---

## 6. 흐름 · Routing · Observability

| 용어 | 쉬운 뜻 |
|---|---|
| 요청 처리 흐름(Request Flow) | HTTP 요청이 들어와 Incident 응답이 나갈 때까지 거치는 처리 과정 |
| 선형 흐름(Linear Flow) | 요청 특성과 관계없이 같은 처리 단계를 거치는 구조 |
| 분기(Branching) | 조건에 따라 서로 다른 처리 경로를 선택하는 것 |
| 라우팅(Routing) | `severity`, `sourceSystem`, `symptoms` 등 요청 특성에 맞춰 경로를 결정하는 것 |
| `route` | 라우팅 결과로 선택된 경로 |
| `QUICK_TRIAGE` | 수업 9에서 정의한 제한적인 LOW·notification-service·증상 조건에 해당하는 빠른 경로. **LLM을 호출하지 않음** |
| `STANDARD_ANALYSIS` | 일반적인 Incident에 대해 필요한 컨텍스트와 LLM 분석·추천을 사용하는 경로 |
| `CRITICAL_ESCALATION` | 심각한 Incident를 정책과 에스컬레이션을 고려해 처리하는 경로. **실제 알림이나 복구 실행을 뜻하지 않음** |
| 관찰 가능성(Observability) | 시스템 내부의 요청 처리와 오류·시간·호출 관계를 외부 기록으로 파악할 수 있는 성질 |
| Observation | Micrometer에서 특정 작업의 관찰 정보를 기록하기 위한 추상화 |
| `@Observed` | 메서드 실행을 Observation으로 기록하도록 지정하는 어노테이션 |
| 추적(Trace) | 하나의 요청과 관련된 작업들을 연결해 보여 주는 실행 기록 |
| `traceId` | 같은 Trace에 속하는 작업을 연결하는 식별자 |
| 구간(Span) | Trace 안에서 수행된 하나의 작업 구간. 시작·종료 시간 및 속성을 가질 수 있음 |
| 서비스 구간(Service Span) | 서비스 메서드 실행을 관찰하기 위해 만든 Span |
| 부모·자식 관계(Parent–Child Span) | 상위 작업과 하위 작업을 연결해 Trace Tree를 구성하는 관계 |
| 이벤트(Event) | Span 안에서 특정 시점에 발생한 작업 이정표. 예: 지식 조회 완료, Prompt 구성 완료 |
| 오류 기록(Error Recording) | Observation/Span에 작업 중 발생한 오류 정보를 남기는 것 |
| 지연 시간(Latency) | 요청이나 작업이 시작해 끝나기까지 걸린 **처리 소요 시간**. Langfuse에서 각 구간을 비교할 때 사용 |
| 메트릭(Metric) | 처리 횟수·시간 등 시스템 상태를 수치로 관찰하는 데이터 |
| 로그(Log) | 실행 중 발생한 메시지와 사건을 텍스트로 기록한 것 |
| 로그 문맥(Mapped Diagnostic Context, MDC) | 로그에 `traceId` 같은 공통 식별자를 함께 표시하도록 돕는 문맥 |
| 로그 상관관계(Log Correlation) | 같은 식별자를 기준으로 여러 로그를 묶어 보는 것 |
| `X-Trace-Id` | 요청 Trace의 식별자를 클라이언트에 알려 주기 위해 사용하는 HTTP 응답 헤더 |
| Micrometer Observation | Spring 애플리케이션의 관찰 데이터를 다루는 Micrometer 추상화 |
| Micrometer Tracing | Trace/Span을 다루고 추적 구현체와 연결하도록 돕는 라이브러리 |
| 개방형 원격 측정(OpenTelemetry, OTel) | Trace·Metric·Log 등을 표준화된 방식으로 다루는 관찰 기술 생태계 |
| OpenTelemetry Protocol(OTLP) | 관찰 데이터를 수집 시스템으로 전달하는 데 사용하는 프로토콜 |
| OTLP Exporter | 애플리케이션에서 생성한 관찰 데이터를 OTLP로 외부 시스템에 전송하는 구성요소 |
| OTLP Endpoint | OTLP Exporter가 데이터를 보내는 수신 주소 |
| Langfuse | LLM 요청의 Trace, Prompt, 모델 호출, 토큰 사용량, 처리 시간 등을 확인하는 관찰 도구 |
| Trace Tree | 하나의 Trace 안에서 부모·자식 Span과 모델 호출의 관계를 계층적으로 보여 주는 화면 |
| Generation | Langfuse에서 LLM 모델 호출을 나타내는 관찰 기록 |
| Token Usage | 모델 호출에 사용된 입력·출력 토큰 수 |
| Spring AI Observation | Spring AI의 모델 호출 등을 관찰 데이터로 남기기 위한 기능 |
| High-Cardinality Attribute | `traceId`나 Prompt 내용처럼 값의 종류가 매우 다양할 수 있는 관찰 속성. Prompt/응답 기록에는 **민감정보 노출 위험**이 있으므로 주의해야 함 |
| `AiObservationSupport` | 강의 코드에서 업무 이벤트와 오류를 현재 관찰 문맥에 기록하도록 돕는 구성요소 |
| `TraceLoggingFilter` | 현재 Trace 식별자를 로그 문맥 및 응답 헤더와 연결하는 필터 |
| 죽은 관찰 지점 | 관찰 코드는 있지만 실제 요청 흐름에서 호출되지 않아 기록이 남지 않는 상태 |

---

## 7. 아키텍처 패턴 도입 판단

| 용어 | 쉬운 뜻 |
|---|---|
| 오케스트레이션(Orchestration) | 여러 작업의 순서·조건·결과를 조정하는 것. **반드시 AI Agent 프레임워크를 사용해야 하는 것은 아님** |
| Orchestrator–Workers | 상위 조정자가 여러 전문 작업(Worker)을 나누어 수행시키고 결과를 모으는 패턴 |
| Orchestrator | 여러 Worker의 작업을 분배하고 결과를 통합하는 조정 역할 |
| Worker | 특정 하위 작업이나 전문 검토를 수행하는 역할 |
| 복합 위험 검토 | 배포 이력, 서비스 의존성, 서로 다른 전문 검토 결과 등을 함께 고려해야 할 수 있는 더 복잡한 검토 상황 |
| 패턴 도입 판단 | 새로운 패턴이 실제 문제를 해결하는지, 기존 Java Workflow를 확장하는 것으로 충분한지 비교하는 과정 |
| Control Map | 요청 흐름의 어느 위치에 Flow·Cost·Quality·Risk 통제가 적용되는지 보여 주는 전체 설계 개념도 |

> **수업 15의 결론:** Orchestrator–Workers는 **도입 여부를 검토하는 대상**이며, Part 1에서 새로 구현한 구성요소가 아닙니다. 현재 요구사항은 기존 Java Workflow와 서비스의 책임 분리로 처리할 수 있습니다. 더 복잡한 요구사항이 생기더라도 먼저 기존 구조의 확장 가능성을 검토합니다.

---

## 8. 자주 혼동하는 개념 빠른 비교

| 혼동하기 쉬운 개념 | 구분 기준 |
|---|---|
| **Baseline vs 잘못된 코드** | Baseline은 동작하는 출발점이며, 통제 장치를 단계적으로 추가하는 대상 |
| **Trace vs Span vs Event** | Trace = 연결된 요청 기록 / Span = 작업 구간 / Event = 구간 안의 특정 순간 |
| **Observation vs OTLP vs Langfuse** | Observation = 내부 관찰 / OTLP = 외부 전송 규약 / Langfuse = 수신·조회·분석 도구 |
| **Latency vs Token Usage** | Latency = 처리 소요 시간 / Token Usage = 입력·출력 토큰 수 |
| **Context Budget vs Token Budget** | Context Budget = 참고 정보의 양을 제한하는 기준 / Token Budget = 토큰 수 기준 한도 |
| **JSON 매핑 vs 출력 계약 vs 의미적 품질** | 객체 변환 가능 여부 / 기본 규칙 준수 여부 / 내용의 근거·적절성 |
| **`PASS` vs `PASSED`** | Evaluator의 판단 / 전체 품질 통제의 최종 상태 |
| **Incident Severity vs Action Risk Level** | 장애가 얼마나 심각한가 / 추천 행동이 얼마나 위험한가 |
| **`requestedSeverity` vs `effectiveSeverity`** | 외부 요청의 심각도 / 정책을 반영한 실제 처리 심각도 |
| **Advisor vs Policy Enforcement** | Prompt에 공통 정책 문맥 제공 / Java 코드로 최종 결과 제한 |
| **`approvalRequired` vs 실제 승인** | 승인 필요 표시 / 외부 승인 절차. Part 1은 표시까지만 구현 |
| **Workflow vs Orchestrator–Workers** | 기존 Java 서비스 조정 구조 / 별도 전문 작업 분담 패턴. 후자는 도입 검토만 진행 |

---

## 9. 수업별 용어 찾아보기

> 아래는 **각 수업에서 특히 알아두면 좋은 용어**입니다. 모든 수업에서 처음 등장한 시점을 정확히 나열한 목록이 아니라, 해당 수업을 이해할 때 유용한 참조표입니다.

| 수업 | 주제 | 먼저 찾아볼 용어 |
|---|---|---|
| **1~4** | 강의 방향과 사례 이해 | LLM, Incident Responder, 통제 4축, Baseline, 책임 분리 |
| **5** | Baseline Incident Responder | Incident, Runbook, Similar Incident, Prompt, `ChatClient`, Adapter, Workflow |
| **6** | 통제 지점 마킹 | Control Point, Marking Point, 출력 계약, Context, 선형 흐름, 정책 |
| **7** | 내부 관찰 구조 | Observability, Observation, `@Observed`, Trace, Span, Event, MDC, `traceId` |
| **8** | Langfuse에서 요청 분석 | OpenTelemetry, OTLP, OTLP Exporter, Langfuse, Trace Tree, Generation, Token Usage, Latency |
| **9** | Routing Workflow | Routing, Branching, `QUICK_TRIAGE`, `STANDARD_ANALYSIS`, `CRITICAL_ESCALATION`, Workflow |
| **10** | Context 비용 통제 | Context Budget, Top N, Capping, Truncation, Deduplication, Re-injection |
| **11** | 기본 출력 계약 검증 | JSON-only, `BeanOutputConverter`, Mapping Failure, Output Contract, Validation, Retry, Fallback |
| **12** | 의미적 품질 평가·개선 | Semantic Quality, Evaluator–Optimizer, Re-Evaluator, `NEEDS_REVISION`, `IMPROVED`, `NOT_RESOLVED` |
| **13** | 위험 추천 분류·정책 | `requestedSeverity`, `effectiveSeverity`, `minimumSeverity`, `ActionRiskClassifier`, `riskLevel`, `approvalRequired`, `IncidentRiskPolicyService` |
| **14** | 공통 정책 문맥과 Advisor | `EscalationDecision`, `IncidentPolicyAdvisor`, `ChatClientRequest` Context, `defaultAdvisors`, Policy Enforcement |
| **15** | Orchestrator–Workers 도입 판단 | Orchestration, Worker, 복합 위험 검토, 패턴 도입 판단 |
| **16** | 최종 Control Map | Control Map, Flow, Cost, Quality, Risk, 책임 분리 |

---

## 10. 이 용어집을 읽을 때 기억할 점

1. **용어집은 코드 설명서가 아닙니다.** 정확한 클래스 호출 관계, 설정 값, 실행 방법은 각 수업의 `codes/README.md`를 우선 확인하세요.
2. **LLM이 판단한 내용과 Java가 강제한 규칙을 구분하세요.** 모델의 평가·추천이 Java의 검증과 정책 적용을 대신하지는 않습니다.
3. **강의용 단순화와 운영 환경의 완전한 안전성 검증은 다릅니다.** 키워드 기반 위험 분류는 통제 지점을 보여 주기 위한 예제입니다.
4. **최종 응답에 포함된 정책 정보가 실제 외부 작업의 실행을 의미하지는 않습니다.** Incident Responder의 책임은 분석·추천·분류·정책 적용·안전한 형태의 응답 전달까지입니다.
