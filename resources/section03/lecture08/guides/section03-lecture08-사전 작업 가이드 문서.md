# 수업 8 사전 작업 — Langfuse Japan Region 가입·프로젝트 생성·API Key·Windows 환경 변수 설정

> 대상 수업: **수업 8. 요청 1건 해부하기 — Langfuse에서 Prompt·RAG·토큰·레이턴시 읽기**
>
> 목적: 수업 8 레코딩을 보기 전에 Langfuse 프로젝트와 인증 정보를 미리 준비합니다. 수업 본문에서는 회원가입 절차를 반복하지 않고, Spring Boot 애플리케이션 설정 → 실행 → Langfuse에서 trace와 service span 분석에 집중합니다.
>
> 이 문서의 기준 환경: **Windows + PowerShell + Langfuse Cloud Japan region**
>
> 완료 기준: 아래 사전 작업을 마치면 수업 8에서 애플리케이션 설정을 추가하고 Incident 요청을 실행했을 때, Langfuse Japan 프로젝트에서 trace를 받을 준비가 된 상태가 됩니다.

---

## 1. 수업 전에 준비하는 이유

수업 8에서는 Spring Boot 애플리케이션이 만든 trace를 Langfuse로 보내고, 장애 대응 요청 한 건을 trace 화면에서 읽습니다.

```text
Incident Responder 애플리케이션
  → OpenTelemetry / OTLP로 trace 전송
  → Langfuse Japan 프로젝트가 trace 수신
  → 수업 8에서 service span, Prompt, token usage, latency 분석
```

이 과정에서 Langfuse 계정, 프로젝트, API key가 없으면 trace를 보낼 목적지와 인증 정보가 없습니다.

그래서 아래 작업은 수업 8 **사전 작업**으로 진행합니다.

```text
1. Langfuse 가입
2. Japan region 선택
3. 실습 프로젝트 생성
4. API key pair 발급
5. Windows 사용자 환경 변수에 key와 endpoint 저장
6. public key와 secret key를 Base64 인코딩해 Authorization 값 생성
7. Authorization 값을 Windows 사용자 환경 변수에 영구 저장
```

> 수업 8 본문에서는 이 준비가 완료됐다고 가정합니다. 실제 수업에서는 애플리케이션 OTLP 설정, 애플리케이션 실행, Langfuse trace와 service span 분석에 집중합니다.

---

## 2. 준비물

- Langfuse Cloud에 가입할 수 있는 이메일 또는 로그인 수단
- Windows PC
- Windows Terminal 또는 PowerShell
- 수업 8에서 사용할 Incident Responder 프로젝트
- API key를 안전하게 보관할 수 있는 개인 개발 환경

> 이 문서는 Windows 기준입니다. macOS 사용자는 Base64 인코딩 명령만 별도 안내를 참고하고, key와 Authorization 값은 사용하는 terminal/IDE의 환경 변수 설정 방식에 맞게 등록하세요.

---

## 3. Langfuse Cloud와 region

### 3.1 region이란 무엇인가

Langfuse Cloud는 trace, Prompt, evaluation 같은 관찰 데이터를 저장하는 위치에 따라 여러 region을 제공합니다.

```text
EU region
US region
Japan region
```

region마다 Langfuse Cloud의 웹 주소와 OTLP endpoint가 다릅니다.

```text
Japan region 웹 주소
https://jp.cloud.langfuse.com

EU region 웹 주소
https://cloud.langfuse.com

US region 웹 주소
https://us.cloud.langfuse.com
```

### 3.2 이 강의에서 선택할 region: Japan

이 강의와 실습에서는 **Japan region**을 선택합니다.

```text
Langfuse Cloud Japan region
웹 주소: https://jp.cloud.langfuse.com
AWS location: Tokyo, ap-northeast-1
```

선택 이유:

- 한국에서 개인 학습과 강의 실습을 진행할 때 지리적으로 가까운 Asia-Pacific region입니다.
- 실습용 trace와 Prompt 데이터가 Japan region에 저장됩니다.
- 강의에서 수강생이 동일한 URL과 endpoint를 사용하도록 안내하기 쉽습니다.

> region은 기능 우열을 고르는 옵션이 아닙니다. 데이터가 저장되는 위치와 접속·전송 주소를 정하는 선택입니다.

### 3.3 가장 중요한 원칙: 프로젝트·key·endpoint는 같은 region

Langfuse region은 서로 분리된 환경입니다.

```text
Japan region에서 만든 프로젝트
  → Japan region에서 발급한 public key / secret key 사용
  → Japan region OTLP endpoint로 전송
  → Japan region 웹 주소에서 trace 확인
```

아래처럼 region을 섞으면 인증에 실패할 수 있습니다.

```text
잘못된 예시

Japan region에서 프로젝트 생성
  → Japan project key 발급
  → EU endpoint인 cloud.langfuse.com으로 trace 전송
  → HTTP 401 Unauthorized 가능
```

이유는 Japan 프로젝트의 key를 EU 서버로 보내면, EU 서버는 해당 Japan 프로젝트와 key를 알지 못하기 때문입니다.

정리하면:

| 항목 | 이 강의의 값 |
|---|---|
| Langfuse 로그인 주소 | `https://jp.cloud.langfuse.com` |
| 프로젝트 생성 위치 | Japan region |
| public key / secret key | Japan region 프로젝트에서 발급 |
| OTLP traces endpoint host | `jp.cloud.langfuse.com` |
| trace 확인 위치 | Japan region Langfuse 프로젝트 |

> **region이 다르면 인증이 실패할 수 있습니다.** 프로젝트를 만든 region, 해당 프로젝트에서 만든 key, exporter가 보내는 endpoint host는 반드시 같은 region을 사용하세요.

### 3.4 endpoint는 반드시 프로젝트 화면에서 최종 확인

이 문서에는 Japan region의 대표 endpoint 예시를 제공합니다. 하지만 Langfuse Cloud의 ingestion 방식과 endpoint 경로는 버전별로 변경될 수 있습니다.

따라서 다음 원칙을 지킵니다.

```text
문서의 endpoint 예시
  → 출발점

자신의 Japan 프로젝트 OpenTelemetry integration 화면에 표시되는 endpoint
  → 최종 기준
```

수업 전 마지막으로 Langfuse Japan 프로젝트의 OpenTelemetry/OTLP integration 화면을 열고 endpoint와 필요한 header를 확인하세요.

---

## 4. Langfuse Cloud 가입

### 4.1 Japan region 접속

브라우저에서 아래 주소로 접속합니다.

```text
https://jp.cloud.langfuse.com
```

Japan region 프로젝트를 만들려면 반드시 이 주소에서 가입·로그인해야 합니다.

### 4.2 가입 절차

1. `https://jp.cloud.langfuse.com`에 접속합니다.
2. 이메일 또는 지원되는 로그인 방식으로 가입합니다.
3. 이메일 인증이 필요하면 인증을 완료합니다.
4. 로그인 뒤 organization 또는 workspace 생성 화면이 나오면 기본 설정으로 생성합니다.

여기서 목표는 Langfuse의 모든 기능을 배우는 것이 아닙니다. **수업 8에서 trace를 받을 실습 공간을 하나 만드는 것**입니다.

### 4.3 가입 완료 확인

- Japan region Langfuse 대시보드가 열리는가
- 주소 표시줄 host가 `jp.cloud.langfuse.com`인가
- organization/workspace에 접근할 수 있는가

---

## 5. 실습 프로젝트 만들기

### 5.1 프로젝트 생성

로그인 후 Japan region Langfuse에서 새 프로젝트를 만듭니다.

권장 프로젝트 이름:

```text
controlled-java-ai-lab
```

이미 같은 프로젝트가 있거나 실습과 녹화 데이터를 분리하고 싶다면 아래처럼 만들 수 있습니다.

```text
controlled-java-ai-lab-dev
```

### 5.2 프로젝트를 따로 만드는 이유

Langfuse trace는 프로젝트 단위로 쌓입니다.

```text
프로젝트 A
  → A 프로젝트 key로 전송한 trace만 표시

프로젝트 B
  → B 프로젝트 key로 전송한 trace만 표시
```

학습 프로젝트를 따로 만들면 실습 trace가 다른 데이터와 섞이지 않습니다. 수업 8에서 방금 실행한 Incident 요청 trace를 찾기 쉬워집니다.

### 5.3 프로젝트 생성 완료 확인

프로젝트를 만든 뒤 아래를 확인합니다.

- 프로젝트 대시보드가 열리는가
- trace 목록 또는 trace 메뉴에 접근할 수 있는가
- project settings에 접근할 수 있는가
- 주소 표시줄 host가 계속 `jp.cloud.langfuse.com`인가

아직 trace가 하나도 없어도 정상입니다. 수업 8에서 애플리케이션을 연결하고 요청을 실행하면 trace가 생성됩니다.

---

## 6. API key pair 발급

### 6.1 key를 발급하는 위치

생성한 **Japan region 프로젝트**의 settings 또는 API keys 메뉴로 이동합니다.

현재 실습 프로젝트용 API key pair를 새로 생성합니다.

발급받을 값:

```text
Public Key
Secret Key
```

화면 구조나 메뉴 이름은 Langfuse 버전에 따라 조금 다를 수 있습니다. 중요한 것은 organization 전체 공용 key가 아니라, **현재 Japan region 실습 프로젝트에 연결된 key pair**를 발급받는 것입니다.

### 6.2 두 key의 역할

| 값 | 역할 | 관리 원칙 |
|---|---|---|
| Public Key | 어느 Langfuse 프로젝트로 trace를 보낼지 식별하는 데 사용 | 공개를 권장하지는 않지만 secret key보다 민감도는 낮음 |
| Secret Key | 애플리케이션의 trace 전송 권한을 인증 | 비밀번호처럼 관리, 절대 Git/코드/강의 화면에 노출하지 않음 |

두 key는 OTLP 요청의 HTTP Basic Authentication 값을 만드는 데 함께 사용됩니다.

```text
public key + ":" + secret key
  → Base64 인코딩
  → Basic Authorization 값
  → OTLP exporter가 Langfuse Japan endpoint로 trace를 전송할 때 사용
```

### 6.3 발급 직후 주의사항

- secret key는 한 번만 전체 값이 표시될 수 있습니다.
- secret key를 안전한 곳에 복사한 뒤 화면에 남기지 않습니다.
- key를 코드, `application.yml`, Git repository, 공개 Notion 문서, 공개 Slack 채널에 붙여 넣지 않습니다.
- 강의 녹화와 화면 공유에서 key가 보이지 않게 합니다.
- key가 노출됐다고 판단되면 Japan region Langfuse project settings에서 해당 key를 revoke/rotate하고 새 key pair를 발급받습니다.

> Base64로 인코딩한 Authorization 값도 secret key를 되돌릴 수 있으므로 같은 수준의 민감 정보로 관리해야 합니다.

---

## 7. OTLP endpoint 확인

### 7.1 endpoint란 무엇인가

수업 8에서 Spring Boot 애플리케이션은 OpenTelemetry trace/span 데이터를 OTLP 방식으로 Langfuse에 보냅니다.

이때 exporter가 trace를 보낼 목적지 주소가 OTLP endpoint입니다.

```text
Spring Boot 애플리케이션
  → OTLP exporter
  → OTLP traces endpoint
  → Langfuse Japan 프로젝트
```

endpoint는 단순한 Langfuse 웹 화면 주소가 아닙니다. **trace 데이터를 받는 전용 ingestion 주소**입니다.

### 7.2 Japan region endpoint 환경 변수 등록


아래 엔드포인트 URL을 확인하세요.

```text
https://jp.cloud.langfuse.com/api/public/otel/v1/traces
```

이 endpoint를 `LANGFUSE_OTLP_TRACES_ENDPOINT` 이름의 환경 변수로 저장합니다. 
환경 변수 저장 방법은 아래 [8.5 OTLP traces endpoint 저장]을 참고하세요.


> EU endpoint인 `https://cloud.langfuse.com/...`나 US endpoint인 `https://us.cloud.langfuse.com/...`를 사용하지 마세요. Japan 프로젝트 key를 다른 region endpoint로 보내면 HTTP 401 Unauthorized 오류가 발생할 수 있습니다.

### 7.3 endpoint도 환경 변수에 저장하는 이유

endpoint는 secret은 아니지만, `application.yml`에 환경별 주소를 하드코딩하면 개발·실습·운영 환경을 바꾸기 어렵습니다.

그래서 이 강의에서는 endpoint도 환경 변수로 분리합니다.

```yaml
endpoint: ${LANGFUSE_OTLP_TRACES_ENDPOINT}
```

---

## 8. Windows 사용자 환경 변수에 원본 값 저장

> 이 절은 Windows 기준입니다.
>
> 환경 변수는 key와 endpoint를 코드·YAML에서 분리하는 방법입니다. 수업 8에서 애플리케이션은 실제 key를 코드에 직접 쓰지 않고 환경 변수에서 값을 읽습니다.

### 8.1 왜 사용자 변수(User scope)를 쓰는가

Windows에는 Process, User, Machine/System 범위의 환경 변수가 있습니다.

| 범위 | 의미 | 이 사전 작업에서의 판단 |
|---|---|---|
| Process | 현재 PowerShell 창을 닫으면 사라지는 임시 변수 | 사용하지 않음 |
| User | 현재 Windows 사용자에게 영구 저장 | 사용 |
| Machine/System | PC의 모든 사용자에게 적용, 관리자 권한이 필요할 수 있음 | 사용하지 않음 |

수강생 개인 PC에서는 User scope가 가장 단순합니다.

- 관리자 권한이 필요하지 않습니다.
- 현재 Windows 계정에서 새로 실행한 Terminal, IntelliJ, Gradle이 값을 읽을 수 있습니다.
- 시스템 전체에 secret을 불필요하게 노출하지 않습니다.

### 8.2 PowerShell 열기

Windows Terminal에서 PowerShell을 열어 아래 명령을 실행합니다.

> 이미 Windows 환경 변수 GUI에서 값을 User scope로 저장했다면 이 절의 PowerShell 저장 명령은 건너뛰어도 됩니다.

### 8.3 public key 저장

아래 `<...>` 부분만 실제 **Japan region 프로젝트의 public key**로 바꿉니다.

```powershell
[System.Environment]::SetEnvironmentVariable(
    "LANGFUSE_PUBLIC_KEY",
    "<LANGFUSE_PUBLIC_KEY>",
    [System.EnvironmentVariableTarget]::User
)
```

### 8.4 secret key 저장

아래 `<...>` 부분만 실제 **Japan region 프로젝트의 secret key**로 바꿉니다.

```powershell
[System.Environment]::SetEnvironmentVariable(
    "LANGFUSE_SECRET_KEY",
    "<LANGFUSE_SECRET_KEY>",
    [System.EnvironmentVariableTarget]::User
)
```

### 8.5 OTLP traces endpoint 저장

아래 값은 Japan region의 대표 예시입니다. 자신의 Japan 프로젝트 OpenTelemetry integration 화면에서 복사한 값을 사용합니다.

```powershell
[System.Environment]::SetEnvironmentVariable(
    "LANGFUSE_OTLP_TRACES_ENDPOINT",
    "https://jp.cloud.langfuse.com/api/public/otel",
    [System.EnvironmentVariableTarget]::User
)
```

### 8.6 명령이 하는 일

아래 구조를 이해하면 됩니다.

```powershell
[System.Environment]::SetEnvironmentVariable(
    "환경변수이름",
    "저장할값",
    [System.EnvironmentVariableTarget]::User
)
```

```text
SetEnvironmentVariable
  → Windows에 환경 변수를 저장하는 기능

"환경변수이름"
  → 나중에 Spring Boot가 읽을 이름

"저장할값"
  → Langfuse key 또는 endpoint

User
  → 현재 Windows 사용자 계정에 영구 저장
```

---

## 9. Authorization 값 만들기와 영구 저장

### 9.1 왜 Authorization 값을 따로 만드는가

Langfuse OTLP endpoint는 public key와 secret key를 각각 별도 header로 받지 않습니다.

두 값을 HTTP Basic Authentication 형식으로 합쳐 `Authorization` header에 넣어야 합니다.

```text
LANGFUSE_PUBLIC_KEY
+ ":"
+ LANGFUSE_SECRET_KEY
        ↓
publicKey:secretKey
        ↓
Base64 인코딩
        ↓
Basic <Base64 결과>
        ↓
LANGFUSE_OTLP_AUTHORIZATION
```

### 9.2 PowerShell에서 Base64 Authorization 생성

아래 코드는 앞에서 Windows User scope에 저장한 public key와 secret key를 읽고, 최종 Authorization 값을 만듭니다.

```powershell
$publicKey = $env:LANGFUSE_PUBLIC_KEY
$secretKey = $env:LANGFUSE_SECRET_KEY

$rawCredentials = "${publicKey}:${secretKey}"
$bytes = [System.Text.Encoding]::UTF8.GetBytes($rawCredentials)
$base64Credentials = [Convert]::ToBase64String($bytes)

$authorization = "Basic $base64Credentials"
```

코드 흐름:

```text
$env:LANGFUSE_PUBLIC_KEY
  → Windows 환경 변수에서 Japan 프로젝트 public key를 읽음

$env:LANGFUSE_SECRET_KEY
  → Windows 환경 변수에서 Japan 프로젝트 secret key를 읽음

"${publicKey}:${secretKey}"
  → HTTP Basic Auth가 요구하는 username:password 형식으로 연결

UTF8.GetBytes(...)
  → 문자열을 Base64 인코딩 가능한 byte 배열로 변환

ToBase64String(...)
  → byte 배열을 Base64 문자열로 인코딩

"Basic $base64Credentials"
  → HTTP Authorization header에 사용할 최종 값 생성
```

> `$authorization`을 단독으로 실행하면 실제 Authorization 값이 터미널에 출력됩니다. 이 값은 민감 정보이므로 화면 녹화나 화면 공유 중에는 출력하지 마세요.

### 9.3 Authorization 값을 Windows 사용자 변수에 영구 저장

바로 이어서 아래 명령을 실행합니다.

```powershell
[System.Environment]::SetEnvironmentVariable(
    "LANGFUSE_OTLP_AUTHORIZATION",
    $authorization,
    [System.EnvironmentVariableTarget]::User
)
```

이 명령은 `$env:LANGFUSE_OTLP_AUTHORIZATION = $authorization`과 다릅니다.

| 명령 | 저장 범위 | 터미널을 닫으면 | Windows 환경 변수 화면 |
|---|---|---|---|
| `$env:NAME = VALUE` | 현재 Process | 사라짐 | 표시되지 않음 |
| `SetEnvironmentVariable(..., User)` | 현재 User | 유지됨 | 사용자 변수에 표시됨 |

이 사전 작업에서는 **User scope 영구 저장 방식**을 사용합니다.

### 9.4 저장 완료 후 재시작

Windows 환경 변수를 새로 저장해도 이미 열려 있던 프로그램은 자동으로 새 값을 받지 않습니다.

아래 순서로 진행하세요.

1. 현재 Windows Terminal/PowerShell 창을 닫습니다.
2. IntelliJ를 사용 중이라면 IntelliJ도 완전히 종료합니다.
3. 새 Windows Terminal/PowerShell 창을 엽니다.
4. IntelliJ를 다시 실행합니다.

새로 시작한 프로그램만 변경된 User 환경 변수를 상속받습니다.

### 9.5 실제 값을 출력하지 않고 확인하기

새 PowerShell에서 아래 명령을 실행합니다.

```powershell
$requiredVariables = @(
    "LANGFUSE_PUBLIC_KEY",
    "LANGFUSE_SECRET_KEY",
    "LANGFUSE_OTLP_TRACES_ENDPOINT",
    "LANGFUSE_OTLP_AUTHORIZATION"
)

foreach ($name in $requiredVariables) {
    if ([string]::IsNullOrWhiteSpace([System.Environment]::GetEnvironmentVariable($name))) {
        throw "$name 환경 변수를 찾을 수 없습니다. 새 PowerShell 창을 열었는지 확인하세요."
    }
}

"Langfuse Japan 사전 작업 환경 변수를 모두 확인했습니다."
```

이 명령은 값 자체를 출력하지 않고 필요한 환경 변수가 존재하는지만 확인합니다.

---

## 10. macOS 사용자를 위한 Base64 명령

macOS에서 `LANGFUSE_PUBLIC_KEY`와 `LANGFUSE_SECRET_KEY`를 이미 환경 변수에 저장했다면 아래 명령으로 Base64 결과를 만들 수 있습니다.

```zsh
printf '%s' "${LANGFUSE_PUBLIC_KEY}:${LANGFUSE_SECRET_KEY}" | base64
```

출력된 문자열 앞에 `Basic `을 붙입니다.

```text
Base64 출력
cGstbGYtLi4uOnNrLWxmLS4uLg==

저장할 Authorization 값
Basic cGstbGYtLi4uOnNrLWxmLS4uLg==
```

`printf '%s'`를 사용하는 이유는 마지막 줄바꿈 문자가 인코딩 대상에 포함되는 것을 막기 위해서입니다.

macOS에서는 zsh 설정 파일, IDE Run Configuration, 비밀 관리 도구 등 자신의 개발 환경에 맞는 방식으로 `LANGFUSE_OTLP_AUTHORIZATION`을 저장합니다.

> macOS에서도 Japan region 프로젝트의 key와 `jp.cloud.langfuse.com` endpoint를 같은 조합으로 사용해야 합니다.

---

## 11. 사전 작업 완료 체크리스트

수업 8 시작 전 아래 항목을 모두 확인하세요.

### Langfuse Japan 계정과 프로젝트

- [ ] `https://jp.cloud.langfuse.com`에서 Langfuse Cloud 가입과 로그인 완료
- [ ] Japan region에서 organization/workspace 생성 완료
- [ ] Japan region에서 실습용 프로젝트 생성 완료
- [ ] trace 목록 또는 trace 메뉴에 접근 가능
- [ ] project settings에 접근 가능

### API key와 region 일치

- [ ] 현재 프로젝트가 Japan region 프로젝트임을 확인
- [ ] Japan region 프로젝트의 public key 발급 완료
- [ ] Japan region 프로젝트의 secret key 발급 완료
- [ ] Japan region 프로젝트 OpenTelemetry/OTLP traces endpoint 확인 완료
- [ ] endpoint host가 `jp.cloud.langfuse.com`인지 확인
- [ ] EU 또는 US endpoint를 사용하지 않음
- [ ] 프로젝트 region, key 발급 region, endpoint region이 모두 Japan으로 일치

### Windows 환경 변수

- [ ] `LANGFUSE_PUBLIC_KEY`를 User scope에 저장
- [ ] `LANGFUSE_SECRET_KEY`를 User scope에 저장
- [ ] `LANGFUSE_OTLP_TRACES_ENDPOINT`를 User scope에 저장
- [ ] public key와 secret key를 Basic Base64 형식으로 변환
- [ ] `LANGFUSE_OTLP_AUTHORIZATION`을 User scope에 영구 저장
- [ ] Terminal과 IntelliJ를 완전히 종료 후 다시 시작
- [ ] 실제 값을 출력하지 않고 네 환경 변수 존재 여부 확인

### 보안

- [ ] secret key를 화면 공유, 녹화, 스크린샷에 노출하지 않음
- [ ] Base64 Authorization 값도 secret과 동일하게 관리
- [ ] `.env`나 별도 local file을 쓴다면 Git ignore 처리
- [ ] key 노출 시 Japan region project settings에서 revoke/rotate해야 함을 이해

---

## 12. 자주 발생하는 문제

### 12.1 `Waiting for first trace`가 계속 보일 때

아래 순서로 확인합니다.

```text
1. 단순 앱 실행만 한 것이 아니라, 실제 ChatClient를 호출하는 Incident API 요청을 보냈는가?
2. Spring Boot OTLP exporter 의존성과 application.yml 설정을 수업 8 내용대로 추가했는가?
3. endpoint host가 jp.cloud.langfuse.com인가?
4. Japan 프로젝트에서 발급한 public/secret key로 Authorization을 만들었는가?
5. Terminal/IntelliJ를 환경 변수 저장 뒤 재시작했는가?
6. application.yml에 x-langfuse-ingestion-version: "4"을 설정했는가?
7. 애플리케이션 로그에 exporter 오류가 있는가?
```

### 12.2 HTTP 401 Unauthorized가 발생할 때

가장 먼저 **region 조합**을 확인합니다.

```text
Japan project key
+ Japan OTLP endpoint
+ Japan Langfuse 웹 화면
= 정상 조합
```

```text
Japan project key
+ EU 또는 US OTLP endpoint
= HTTP 401 Unauthorized 가능
```

그 다음 Authorization 형식을 확인합니다.

```text
publicKey:secretKey 순서인가?
두 값 사이에 : 하나가 있는가?
Base64 인코딩 대상에 공백이나 줄바꿈이 없는가?
앞에 Basic 공백이 포함됐는가?
key를 새로 발급/rotate한 뒤 Authorization을 다시 만들었는가?
```

### 12.3 환경 변수를 찾지 못할 때

확인:

- 환경 변수를 User scope로 저장했는지 확인합니다.
- 환경 변수 저장 뒤 새 PowerShell을 열었는지 확인합니다.
- IntelliJ를 재시작했는지 확인합니다.
- 환경 변수 이름 철자를 확인합니다.

### 12.4 endpoint가 맞는지 모르겠을 때

아래 Langfuse 공식 문서에서 endpoint를 확인합니다.  

[Langfuse 엔드포인트](https://langfuse.com/integrations/native/opentelemetry?utm_source=chatgpt.com)

### 12.5 key가 노출됐을 때

1. Japan region Langfuse project settings에서 해당 key를 revoke 또는 rotate합니다.
2. 새 key pair를 발급합니다.
3. Windows 환경 변수의 public/secret key를 새 값으로 변경합니다.
4. Authorization 값을 다시 만들어 `LANGFUSE_OTLP_AUTHORIZATION`을 덮어씁니다.
5. Terminal과 IntelliJ를 재시작합니다.

---

## 13. 수업 8에서 이어질 내용

사전 작업을 마쳤다면 수업 8에서 아래 작업을 진행합니다.

```text
1. Spring Boot에 OTLP exporter 의존성 추가
2. application.yml에 Japan endpoint와 Authorization 환경 변수 연결
3. Langfuse v4 ingestion header 설정
4. 애플리케이션 실행
5. Incident Responder 정상 요청 한 건 실행
6. Japan Langfuse 프로젝트에서 최신 trace 찾기
7. service span 흐름 분석
8. input/output/total token usage와 latency 읽기
9. 추가적으로 에러 요청 trace 확인
```

수업 8의 핵심은 가입이나 API key 발급이 아닙니다.

> 요청 한 건이 어떤 service span과 LLM generation을 거쳤고, Prompt에 무엇이 들어갔으며, token과 latency가 어디에서 사용됐는지를 Langfuse trace로 읽는 것이 핵심입니다.

---

## 14. 한 페이지 요약

```text
[Langfuse Japan]
1. https://jp.cloud.langfuse.com 접속
2. Japan region에서 Cloud 가입
3. 실습 프로젝트 생성
4. Japan 프로젝트 public key / secret key 발급
5. Japan 프로젝트 OTLP traces endpoint 복사

[Windows User 환경 변수]
6. LANGFUSE_PUBLIC_KEY 저장
7. LANGFUSE_SECRET_KEY 저장
8. LANGFUSE_OTLP_TRACES_ENDPOINT 저장
9. publicKey:secretKey → Base64 → Basic Authorization 생성
10. LANGFUSE_OTLP_AUTHORIZATION 영구 저장
11. Terminal/IntelliJ 재시작

[반드시 지킬 원칙]
12. Japan 프로젝트 key는 Japan endpoint로만 전송
13. EU/US endpoint와 섞으면 HTTP 401 인증 오류가 날 수 있음

[수업 8]
14. 애플리케이션 설정
15. Incident 요청 실행
16. Langfuse에서 service span, token, latency 읽기
```

> 수업 8 시작 전에 여기까지 완료해 두세요. 수업에서는 이 준비를 바탕으로 애플리케이션의 trace를 Langfuse Japan으로 전송하고, 요청 한 건을 실제로 해부합니다.
