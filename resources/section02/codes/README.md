# lecture05-baseline-incident-responder

수업 5에서 사용하는 baseline Incident Responder 앱입니다.  
이 프로젝트의 목적은 “AI가 장애 상황을 어떻게 받아들이고 응답하는가”를 빠르게 체험하는 것이 아니라, **이후 Part 1 전체에서 함께 읽고 해부할 기준 사례를 제공하는 것**입니다.

즉, 이 코드는 완성형 운영 시스템이라기보다, 다음 수업들에서 비용·품질·리스크·흐름 관점으로 계속 보완해 나갈 baseline 앱입니다.

---

## 이 프로젝트에서 하는 일

이 앱은 장애 상황을 설명하는 요청을 입력으로 받아, 다음과 같은 흐름으로 응답을 생성합니다.

1. Incident 요청을 받습니다.
2. 관련 runbook snippet을 조회합니다.
3. 유사 incident 이력을 조회합니다.
4. severity 기반 escalation policy를 조회합니다.
5. 수집한 컨텍스트를 바탕으로 incident 분석을 수행합니다.
6. 분석 결과를 바탕으로 추천 액션을 생성합니다.
7. 구조화된 IncidentResponse를 반환합니다.

핵심은, 단순히 “질문 하나를 LLM에 던지는” 형태가 아니라, **기본적인 orchestration과 context enrichment를 가진 baseline 구조**라는 점입니다.

---

## 수업 5에서의 학습 포인트

수업 5에서는 이 프로젝트를 가지고 아래 내용을 확인합니다.

- 요청이 어디에서 들어오는가
- Controller가 어떤 역할만 담당하는가
- 실제 orchestration은 어느 서비스에서 일어나는가
- AI 호출 전에 어떤 컨텍스트가 모이는가
- 분석 단계와 액션 추천 단계가 어떻게 분리되어 있는가
- Prompt 생성과 ChatClient 호출이 어디에 놓여 있는가
- 왜 이 코드가 이후 Part 1 전체에서 사용할 기준 사례가 되는가

중요한 점은, **수업 5는 구현 수업이 아니라 코드 읽기 수업**이라는 것입니다.  
새로운 기능을 추가하거나 구조를 바꾸는 것이 아니라, baseline의 전체 흐름을 이해하는 데 집중합니다.

---

## 프로젝트 위치

이 프로젝트는 아래 경로에 위치합니다.

```text
resources/
  section02/
    lecture05/
      codes/
        README.md
        lecture05-baseline-incident-responder/
          build.gradle
          settings.gradle
          src/
          ...
```

즉, `lecture05-baseline-incident-responder/` 자체가 실제 앱 프로젝트 루트입니다.

---

## 실행 환경

- Java 21
- Spring Boot 3.x
- Spring AI 1.1.4
- Gradle

## 실행 전 준비

OpenAI API Key가 필요합니다.

환경 변수 예시:

```bash
export OPENAI_API_KEY=your-api-key
```

또는 IDE 실행 설정에서 환경 변수를 지정합니다.

> 사용 중인 `application.yml` 설정에 따라 키 이름이 다를 수 있으니, 실제 프로젝트 설정 파일을 함께 확인하세요.

---

## 실행 방법

### 1) 터미널에서 실행

프로젝트 루트에서 아래 명령을 실행합니다.

```bash
./gradlew bootRun
```

### 2) IDE에서 실행

다음 메인 클래스를 실행합니다.

```text
io.controlled.agents.incidentresponder.IncidentResponderApplication
```

---

## 기본 포트

기본 실행 포트는 다음과 같습니다.

```text
18080
```

따라서 엔드포인트 호출 주소는 아래와 같습니다.

```text
POST http://localhost:18080/api/incidents/respond
```

---

## API 개요

### Endpoint

```http
POST /api/incidents/respond
```

### 설명

장애 상황을 설명하는 JSON 요청을 받아, 분석 결과와 추천 액션이 포함된 구조화된 응답을 반환합니다.

---

## 요청 예시

```json
{
  "sourceSystem": "order-service",
  "title": "Checkout timeouts",
  "summary": "Users cannot complete orders intermittently",
  "severity": "HIGH",
  "symptoms": [
    "checkout API timing out",
    "error rate spike"
  ]
}
```

---

## 응답 예시

아래는 baseline 수준에서 기대할 수 있는 응답 형태의 예시입니다.

```json
{
  "incidentId": "6dd3d0d1-5f0c-4a4f-bb4e-f9a8a4f33f22",
  "severity": "HIGH",
  "analysis": {
    "situationSummary": "order-service에서 checkout 실패가 증가하고 있으며, DB 연결 압박과 관련된 증상으로 보입니다.",
    "likelyCauses": [
      "DB connection pool 포화 가능성",
      "하위 의존 서비스 지연 전파 가능성",
      "최근 트래픽 급증으로 인한 용량 여유 부족 가능성"
    ],
    "impactAssessment": "사용자가 주문을 완료하지 못할 수 있으며, 장애가 지속되면 매출 영향이 발생할 수 있습니다.",
    "confidenceNote": "timeout 및 500 에러 증상 기준으로 중간 수준의 확신을 가집니다."
  },
  "recommendedActions": [
    {
      "priority": 1,
      "actionTitle": "datasource pool 포화와 대기 중인 connection 상태를 우선 점검합니다.",
      "rationale": "현재 증상 조합이 DB connection pressure와 가장 가깝습니다.",
      "ownerSuggestion": "백엔드 온콜 엔지니어"
    },
    {
      "priority": 2,
      "actionTitle": "최근 배포 이력을 확인하고 이상 시 롤백 가능성을 검토합니다.",
      "rationale": "최근 배포가 pool leak 또는 응답 지연을 유발했을 수 있습니다.",
      "ownerSuggestion": "서비스 오너"
    },
    {
      "priority": 3,
      "actionTitle": "현재 사용자 영향 범위를 정리해 이해관계자 공유 준비를 합니다.",
      "rationale": "HIGH 장애는 기술 대응과 함께 커뮤니케이션 준비가 필요합니다.",
      "ownerSuggestion": "Incident commander 또는 팀 리드"
    }
  ],
  "similarIncidents": [
    {
      "incidentId": "INC-2026-0412",
      "title": "Order API timeout due to exhausted DB connections",
      "resolutionSummary": "connection pool 압박을 완화하고 stuck pod를 재시작한 뒤 datasource threshold를 조정해 복구했습니다."
    },
    {
      "incidentId": "INC-2026-0328",
      "title": "Checkout latency spike after traffic burst",
      "resolutionSummary": "hot endpoint 트래픽을 rate-limit하고 replica lag를 점검한 뒤 안정화했습니다."
    }
  ],
  "escalationPolicy": "Page primary on-call immediately and prepare stakeholder communication.",
  "responseNote": "영향도가 큰 운영 조치는 실제 실행 전에 사람 검토가 필요합니다."
}
```

---

## 코드 흐름 빠르게 보기

수업 5에서 가장 먼저 볼 흐름은 아래와 같습니다.

### 1. 진입점

- `incident.adapter.in.IncidentResponderController`

HTTP 요청을 받아 use case로 전달합니다.

### 2. 입력 포트

- `incident.port.in.RespondToIncidentUseCase`

애플리케이션이 외부 요청을 어떻게 받는지 보여주는 입력 포트입니다.

### 3. 메인 orchestration

- `incident.application.IncidentResponderService`

이 baseline의 핵심 흐름을 조립하는 서비스입니다.

주요 역할:

- Incident 생성
- runbook 조회
- 유사 incident 조회
- escalation policy 조회
- 분석 수행
- 액션 추천 수행
- 최종 응답 조립

### 4. 분석 단계

- `incident.application.IncidentAnalysisService`

수집된 컨텍스트를 바탕으로 incident를 분석합니다.

### 5. 액션 추천 단계

- `incident.application.IncidentActionRecommendationService`

분석 결과를 바탕으로 즉시 실행 가능한 조치를 제안합니다.

### 6. AI 호출 관련 클래스

- `incident.adapter.out.ai.IncidentPromptFactory`
- `incident.adapter.out.ai.IncidentChatGateway`

Prompt를 만들고, 실제 ChatClient를 통해 모델을 호출하는 역할을 담당합니다.

### 7. 외부 컨텍스트 adapter

- `incident.adapter.out.knowledge.StaticRunbookKnowledgeAdapter`
- `incident.adapter.out.persistence.InMemoryIncidentHistoryAdapter`
- `incident.adapter.out.policy.StaticAlertPolicyAdapter`

현재 baseline에서는 실제 운영 시스템 연동 대신 static / in-memory 방식으로 단순화되어 있습니다.

---

## 패키지 구조 요약

```text
src/main/java/io/controlled/agents/incidentresponder
├─ common
│  ├─ config
│  ├─ exception
│  └─ util
├─ incident
│  ├─ domain
│  ├─ dto
│  ├─ port
│  │  ├─ in
│  │  └─ out
│  ├─ application
│  └─ adapter
│     ├─ in
│     └─ out
└─ observability
```

### 구조 해석

- `domain`: incident 핵심 도메인 모델
- `dto`: 요청/응답 및 AI 결과 구조
- `port.in`: 애플리케이션이 받는 입력 인터페이스
- `port.out`: 애플리케이션이 외부에 의존하는 인터페이스
- `application`: orchestration과 use case 흐름
- `adapter.in`: HTTP 진입점
- `adapter.out`: AI, knowledge, history, policy 연결
- `observability`: 추적/관측 보조 코드

---

## baseline의 의도적 한계

이 프로젝트는 수업용 baseline이기 때문에 일부를 의도적으로 단순화했습니다.

- 실제 운영 시스템 연동 대신 static / in-memory adapter 사용
- 복잡한 routing 없음
- evaluator / optimizer 없음
- 강한 DTO post-validation 없음
- tool policy gate 없음
- orchestrator-workers 구조 없음
- observability는 최소 baseline 수준

즉, **그럴듯하게 동작하지만 아직 통제는 충분히 들어가 있지 않은 상태**입니다.  
이 점이 오히려 Part 1 이후 수업의 출발점이 됩니다.

---

## 이 프로젝트를 볼 때의 권장 순서

수업 5에서는 아래 순서로 읽는 것을 권장합니다.

1. `IncidentResponderController`
2. `IncidentRequest`, `IncidentResponse`
3. `RespondToIncidentUseCase`
4. `IncidentResponderService`
5. `IncidentAnalysisService`
6. `IncidentActionRecommendationService`
7. `IncidentPromptFactory`
8. `IncidentChatGateway`
9. `StaticRunbookKnowledgeAdapter`
10. `InMemoryIncidentHistoryAdapter`
11. `StaticAlertPolicyAdapter`

이 순서대로 보면 “요청이 들어와서 응답이 만들어지는 과정”이 자연스럽게 연결됩니다.

---

## 수업 5 실습에서 하지 않는 것

이번 수업에서는 아래 작업을 하지 않습니다.

- 새 코드 작성
- 프롬프트 수정 실습
- 구조 리팩터링
- Advisor 추가
- Observability 확장
- DTO validation 고도화
- Routing 분기 구현
- Evaluator / Optimizer 추가
- Tool 연결 및 정책 제어 추가

이런 내용은 이후 수업에서 단계적으로 다룹니다.

---

## 자주 보는 포인트

코드를 읽을 때 특히 아래 질문을 계속 떠올리면 좋습니다.

- 요청은 어디에서 들어오는가?
- 실제 흐름은 어디에서 조립되는가?
- AI 호출 전에 어떤 맥락이 수집되는가?
- 분석과 액션 추천은 왜 분리되어 있는가?
- Prompt 생성과 모델 호출은 어디서 나뉘는가?
- 어떤 부분이 현재 baseline의 한계인가?

---

## 빠른 호출 테스트

앱 실행 후 아래 curl로 빠르게 테스트할 수 있습니다.

```bash
curl -X POST http://localhost:18080/api/incidents/respond \
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

---

## 마무리

이 baseline 앱의 목적은 “최고로 똑똑한 incident responder”를 만드는 것이 아닙니다.  
목적은 **통제 가능한 AI 애플리케이션을 설명하기 위한 기준 사례**를 제공하는 것입니다.

수업 5에서는 이 흐름을 익히고, 이후 수업에서는 같은 코드를 다시 보면서 다음 질문으로 넘어가게 됩니다.

- 관측 가능하게 만들려면 무엇을 더 봐야 하는가
- 흐름을 분기하려면 무엇이 부족한가
- 출력 품질을 더 안정화하려면 무엇이 필요한가
- 위험한 액션을 통제하려면 어디에 장치를 넣어야 하는가

이 README는 그 출발점을 잡기 위한 안내 문서입니다.