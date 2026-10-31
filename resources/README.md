# Spring AI 사례로 배우는 AI 애플리케이션 통제 기법 — Part 1 실습 자료

이 폴더는 **[Part 1] Spring AI 사례로 배우는 AI 애플리케이션 통제 기법: 비용·품질·리스크·흐름 통제** 강의의 수강생용 자료입니다.

강의에서는 장애 상황을 분석하고 대응 액션을 추천하는 **Incident Responder** 애플리케이션을 하나의 사례로 사용합니다. 정상적으로 동작하는 Baseline 애플리케이션에서 시작해 요청 흐름을 관찰하고, LLM에 전달하는 정보를 제한하며, AI 출력의 품질과 위험한 추천을 Java/Spring 애플리케이션에서 통제하는 과정을 학습합니다.

> **학습의 핵심:** AI가 항상 올바른 답을 생성하도록 만드는 것이 아니라, **AI의 판단을 애플리케이션이 관찰·검증·제한하고 정책에 따라 처리할 수 있도록 통제 지점을 설계**하는 것입니다.

## 1. 자료를 이용하는 방법

1. **수강 중인 수업 번호**에 맞는 `sectionXX/lectureXX/` 폴더를 찾습니다.
2. `slides/`의 PDF로 수업 내용을 복습합니다.
3. `codes/`가 있는 수업은 **먼저 `codes/README.md`를 읽습니다.** 해당 수업의 목적, 이전 수업 대비 변경 사항, 실행·테스트 방법과 관찰 포인트가 정리되어 있습니다.
4. 실습 코드를 실행하려면 `codes/lectureXX-incident-responder/`를 **독립적인 Gradle 프로젝트**로 IDE에서 엽니다. `codes/` 자체가 프로젝트 루트는 아닙니다.
5. 수업 8부터 Langfuse를 활용하려면 **수업 8 사전 작업 가이드(guides/section03-lecture08-사전 작업 가이드 문서.md)**를 먼저 확인합니다.

**수업별 실습 코드는 해당 수업의 결과를 반영한 코드 스냅샷입니다.** 각 프로젝트를 순서대로 살펴보면서 어떤 통제 지점이 추가되었는지 비교해 보세요. 반드시 이전 수업의 코드를 직접 수정해 다음 수업의 결과물로 만들 필요는 없습니다.

## 2. 전체 폴더 구조

```text
resources/
├── README.md                         ← 현재 문서
├── glossary.md                       ← 강의 용어집
├── section01/
│   ├── lecture01/slides/
│   ├── lecture02/slides/
│   ├── lecture03/slides/
│   └── lecture04/slides/
├── section02/
│   ├── lecture05/
│   │   ├── slides/
│   │   └── codes/
│   │       ├── README.md
│   │       └── lecture05-incident-responder/
│   └── lecture06/                    ← slides/ + codes/
├── section03/
│   ├── lecture07/                    ← slides/ + codes/
│   ├── lecture08/                    ← slides/ + codes/ + guides/
│   └── lecture09/                    ← slides/ + codes/
├── section04/
│   └── lecture10/                    ← slides/ + codes/
├── section05/
│   ├── lecture11/                    ← slides/ + codes/
│   └── lecture12/                    ← slides/ + codes/
├── section06/
│   ├── lecture13/                    ← slides/ + codes/
│   ├── lecture14/                    ← slides/ + codes/
│   └── lecture15/slides/
└── section07/
    └── lecture16/slides/
```

- **`slides/`**: 해당 수업의 PDF 슬라이드
- **`codes/README.md`**: 해당 수업의 코드 변경 사항과 실습·검증 안내
- **`codes/lectureXX-incident-responder/`**: 해당 수업에서 사용하는 실행 가능한 Spring Boot/Gradle 프로젝트
- **`guides/`**: 수업 전에 완료해야 할 별도 준비 안내 (현재 수업 8에 포함)
- **`glossary.md`**: 강의에서 사용하는 주요 용어 설명

> `section01`의 수업 1~4, `section06/lecture15`, `section07/lecture16`에는 **슬라이드 PDF만** 제공됩니다. 해당 수업에 코드 폴더가 없는 것은 누락이 아닙니다.

## 3. 수업별 자료 안내

| 수업 | 학습 주제 및 제공 자료 | 코드 안내 |
|---|---|---|
| 01 | 강의 도입 · [슬라이드](section01/lecture01/slides/controlled-java-ai-p1-section01-lecture01.pdf) | — |
| 02 | [슬라이드](section01/lecture02/slides/controlled-java-ai-p1-section01-lecture02.pdf) | — |
| 03 | [슬라이드](section01/lecture03/slides/controlled-java-ai-p1-section01-lecture03.pdf) | — |
| 04 | [슬라이드](section01/lecture04/slides/controlled-java-ai-p1-section01-lecture04.pdf) | — |
| 05 | Baseline Incident Responder 구조 읽기 · [슬라이드](section02/lecture05/slides/controlled-java-agent-p1-section02-lecture05.pdf) | [코드 README](section02/lecture05/codes/README.md) |
| 06 | Baseline의 Flow·Cost·Quality·Risk 통제 지점 마킹 · [슬라이드](section02/lecture06/slides/controlled-java-ai-p1-section02-lecture06.pdf) | [코드 README](section02/lecture06/codes/README.md) |
| 07 | trace·span·event 기반 내부 관찰 구조 · [슬라이드](section03/lecture07/slides/controlled-java-ai-p1-section03-lecture07.pdf) | [코드 README](section03/lecture07/codes/README.md) |
| 08 | Langfuse에서 요청 흐름·LLM 호출 분석 · [슬라이드](section03/lecture08/slides/controlled-java-ai-p1-section03-lecture08.pdf) | [사전 준비 가이드](section03/lecture08/guides/section03-lecture08-사전%20작업%20가이드%20문서.md) · [코드 README](section03/lecture08/codes/README.md) |
| 09 | 요청 특성에 따른 Routing Workflow · [슬라이드](section03/lecture09/slides/controlled-java-agent-p1-section03-lecture09.pdf) | [코드 README](section03/lecture09/codes/README.md) |
| 10 | Context Budget으로 LLM 입력 비용 통제 · [슬라이드](section04/lecture10/slides/controlled-java-ai-p1-section04-lecture10.pdf) | [코드 README](section04/lecture10/codes/README.md) |
| 11 | LLM 출력의 JSON·DTO 기본 계약 검증 · [슬라이드](section05/lecture11/slides/controlled-java-ai-p1-section05-lecture11.pdf) | [코드 README](section05/lecture11/codes/README.md) |
| 12 | Evaluator–Optimizer 기반 내용 품질 평가·개선 · [슬라이드](section05/lecture12/slides/controlled-java-ai-p1-section05-lecture12.pdf) | [코드 README](section05/lecture12/codes/README.md) |
| 13 | 위험 추천 분류와 Java 정책 적용 · [슬라이드](section06/lecture13/slides/controlled-java-ai-p1-section06-lecture13.pdf) | [코드 README](section06/lecture13/codes/README.md) |
| 14 | Advisor를 통한 공통 정책 문맥 적용 · [슬라이드](section06/lecture14/slides/controlled-java-ai-p1-section06-lecture14.pdf) | [코드 README](section06/lecture14/codes/README.md) |
| 15 | 복합적인 위험 검토와 Orchestrator–Workers 도입 판단 · [슬라이드](section06/lecture15/slides/controlled-java-ai-p1-section06-lecture15.pdf) | — (기존 구조 분석·설계 판단) |
| 16 | Incident Responder Control Map과 설계 원칙 · [슬라이드](section07/lecture16/slides/controlled-java-ai-p1-section07-lecture16.pdf) | — (Part 1 마무리) |

수업별 자세한 클래스 설명, 요청 예시, 테스트 시나리오, 이전 수업 대비 변경 사항은 위 표의 **코드 README**에서 확인해 주세요.

## 4. 실습 환경과 실행

수업 5~14에 제공된 프로젝트의 `build.gradle`을 기준으로 다음 환경을 사용합니다.

- **Java 21**
- **Spring Boot 3.5.13**
- **Spring AI 1.1.4**
- **Gradle Wrapper** (프로젝트에 포함)
- **OpenAI API Key** (실제 모델 호출 시 필요)
- **IDE**: IntelliJ IDEA 등 Gradle 기반 Java/Spring 프로젝트를 열 수 있는 개발 도구
- **API 호출 도구**: Postman 또는 HTTP 요청을 보낼 수 있는 도구

### 4.1 프로젝트 열기

예를 들어 수업 10을 실습한다면 다음 폴더를 **프로젝트 루트**로 엽니다.

```text
resources/section04/lecture10/codes/lecture10-incident-responder/
```

`build.gradle`, `settings.gradle`, `gradlew`, `src/`가 있는 폴더입니다. 다른 수업을 실습할 때는 해당 수업의 `lectureXX-incident-responder/` 폴더를 엽니다.

### 4.2 OpenAI API Key 설정

프로젝트의 `src/main/resources/application.yaml`은 `OPENAI_API_KEY` 환경 변수를 사용합니다. 키는 개인 환경 변수 또는 IDE 실행 설정에 등록해 주세요.

```bash
# macOS / Linux 터미널 예시
export OPENAI_API_KEY=your-api-key
```

Windows에서는 IDE의 Run Configuration 또는 운영체제 환경 변수 설정을 이용할 수 있습니다. **실제 API 키를 소스코드, README, 공개 저장소에 기록하지 마세요.** 모델 호출에는 이용 중인 API 서비스의 요금이 발생할 수 있습니다.

### 4.3 애플리케이션 실행

해당 수업의 **프로젝트 루트**에서 실행합니다.

```bash
# macOS / Linux
./gradlew bootRun

# Windows PowerShell
.\gradlew.bat bootRun
```

IDE에서 `IncidentResponderApplication`의 `main` 메서드를 실행해도 됩니다. 자세한 실행·검증 순서는 수업별 `codes/README.md`를 따르세요.

기본 설정의 API 주소는 다음과 같습니다.

```http
POST http://localhost:18080/api/incidents/respond
```

요청 JSON은 수업별 README의 예시를 사용하세요. **수업이 진행되면서 요청·응답 필드와 통제 정책이 달라질 수 있으므로**, 다른 수업의 예시를 그대로 재사용하지 않는 편이 좋습니다.

> 모든 수업 프로젝트가 기본적으로 같은 포트 `18080`을 사용합니다. 다른 수업 프로젝트로 전환할 때는 이전에 실행한 애플리케이션을 종료한 뒤 실행하세요.

## 5. Langfuse 실습 전 준비 (수업 8 이후)

수업 7에서는 애플리케이션 내부의 trace·span·event를 확인하고, **수업 8부터** OTLP exporter로 Langfuse에 관찰 데이터를 전송합니다.

수업 8을 시작하기 전에 다음 문서를 먼저 읽어 주세요.

**[수업 8 — Langfuse 사전 작업 가이드](section03/lecture08/guides/section03-lecture08-사전%20작업%20가이드%20문서.md)**

이 가이드는 강의에서 사용하는 **Langfuse Cloud Japan region**을 기준으로 프로젝트 생성, API Key 발급, OTLP endpoint, 인증값 및 Windows 환경 변수 설정을 설명합니다.

수업 8 이후 코드에서 사용되는 주요 환경 변수는 다음과 같습니다.

| 환경 변수 | 용도 |
|---|---|
| `OPENAI_API_KEY` | OpenAI 모델 호출 인증 |
| `LANGFUSE_OTLP_TRACES_ENDPOINT` | Langfuse OTLP trace 전송 주소 |
| `LANGFUSE_OTLP_AUTHORIZATION` | OTLP 전송 Authorization 헤더 |

**Langfuse 웹 UI 주소와 OTLP trace 수신 주소는 다릅니다.** 프로젝트·API Key·endpoint의 region을 일치시키고, 실제 endpoint와 인증 방법은 가이드 및 사용하는 Langfuse 프로젝트의 설정을 확인해 주세요.

`LANGFUSE_OTLP_AUTHORIZATION` 값은 Base64 인코딩 형태라도 민감한 인증 정보이므로 외부에 공유하지 마세요. Langfuse 사용량 및 과금 정책은 서비스의 현재 정책을 확인해 주세요.

## 6. 학습할 때 알아둘 점

### 코드 스냅샷을 비교해 보세요

- **수업 5~6:** 정상 동작하는 Baseline을 읽고 통제 지점을 찾습니다. 수업 6의 주석 마킹은 기능 변경이 아닙니다.
- **수업 7~9:** 요청을 관찰하고, 실제 요청 특성에 따라 처리 경로를 선택합니다.
- **수업 10:** Context의 선택량·크기를 제한하고 개선 전후를 비교합니다.
- **수업 11~12:** 기본 출력 계약과 의미적 내용 품질을 구분하여 검증합니다.
- **수업 13~14:** 위험 추천에 Java 정책을 적용하고 공통 정책 문맥을 LLM 호출에 전달합니다.
- **수업 15~16:** 새로운 코드를 추가하기보다 기존 구조를 분석하고 전체 통제 설계 원칙을 정리합니다.

### 강의에서 다루지 않는 범위

Incident Responder는 **대응 액션을 추천**하는 애플리케이션입니다. 실제 서버 재시작·롤백·인프라 변경, 승인 처리, 승인 후 실행, 자동 복구를 수행하지 않습니다. `approvalRequired`와 같은 값은 **추천에 대한 정책 정보**이지, 실제 승인 절차의 구현을 뜻하지 않습니다.

또한 수업 15의 Orchestrator–Workers는 **도입 필요성을 검토하는 주제**이며, 별도 구현 프로젝트가 제공되는 수업은 아닙니다.

## 7. 문제가 생겼을 때

| 상황 | 먼저 확인할 사항 |
|---|---|
| 프로젝트가 열리지 않거나 빌드되지 않음 | `codes/`가 아닌 `lectureXX-incident-responder/`를 열었는지, Java 21이 설정되었는지 확인 |
| OpenAI 인증 오류 | `OPENAI_API_KEY`가 **실행 프로세스**에 전달되었는지 확인 |
| `18080` 포트 충돌 | 다른 수업의 Incident Responder가 실행 중인지 확인 |
| 요청 예시가 현재 코드와 맞지 않음 | **해당 수업**의 `codes/README.md`에 있는 요청·응답 예시 사용 |
| Langfuse에 trace가 보이지 않음 | 수업 8 사전 작업 가이드, OTLP endpoint, Authorization, region 일치 여부 확인 |
| 테스트 결과가 강의 화면과 다름 | 해당 수업 README의 테스트 목적·전제 조건 확인. LLM 출력은 실행마다 달라질 수 있음 |

더 자세한 오류 원인과 검증 절차는 각 수업의 `codes/README.md`를 참고하세요.

## 8. 용어집

강의에서 등장하는 **Observability, Trace, Span, RAG, Context Budget, Evaluator–Optimizer, Advisor** 등의 용어는 [glossary.md](glossary.md)에서 확인할 수 있습니다.

---

**이 자료는 강의 순서에 맞춰 각 통제 지점을 살펴보도록 구성되어 있습니다.** 먼저 수업의 문제를 이해하고, 해당 코드 README와 프로젝트를 함께 보면서 *무엇을 통제하기 위해 이 변경이 필요했는지*를 확인해 보세요.
