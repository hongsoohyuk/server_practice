# Phase 7 - 인증/인가와 배포 (선택)

## 학습 목표
- 인증(Authentication)과 인가(Authorization)의 차이를 이해한다
- JWT 기반 인증의 원리를 학습한다
- Docker로 애플리케이션을 컨테이너화하는 방법을 익힌다
- 클라우드 배포의 기본 흐름을 이해한다

---

## 1. 인증(Authentication) vs 인가(Authorization)

### 1.1 개념 구분

```
인증 (Authentication)           인가 (Authorization)
"너 누구야?"                    "너 이거 해도 돼?"
─────────────────              ─────────────────
로그인                          권한 확인
신원 확인                       접근 제어
ID/Password, 지문, 토큰         Role, Permission
```

예시:
- **인증**: 로그인해서 "나는 홍길동이다"를 증명
- **인가**: 홍길동이 "관리자 페이지에 접근할 수 있는가?" 확인

### 1.2 세션 기반 vs 토큰 기반

#### 세션 기반 인증

```
Client                          Server
  │                               │
  │  1) 로그인 요청                │
  │  POST /login                  │
  │  {id: "kim", pw: "1234"}     │
  ├──────────────────────────────>│
  │                               │ 2) 세션 생성 (서버 메모리에 저장)
  │  3) 세션 ID 쿠키로 전달       │    session_id: "abc123"
  │  Set-Cookie: SID=abc123       │    user: "kim"
  │<──────────────────────────────│
  │                               │
  │  4) 이후 요청마다 쿠키 자동 전송│
  │  Cookie: SID=abc123           │
  ├──────────────────────────────>│ 5) 세션 확인
  │                               │
```

문제점:
- 서버가 세션을 메모리에 저장 → 서버가 여러 대면 **세션 공유** 필요
- 서버 재시작 시 세션 소멸
- 모바일 앱에서 쿠키 사용이 불편

#### 토큰 기반 인증 (JWT)

```
Client                          Server
  │                               │
  │  1) 로그인 요청                │
  │  POST /login                  │
  │  {id: "kim", pw: "1234"}     │
  ├──────────────────────────────>│
  │                               │ 2) JWT 생성 (서버에 저장 안 함!)
  │  3) JWT 토큰 반환              │
  │  {"token": "eyJhbGci..."}    │
  │<──────────────────────────────│
  │                               │
  │  4) 이후 요청마다 헤더에 토큰   │
  │  Authorization: Bearer eyJ... │
  ├──────────────────────────────>│ 5) 토큰 검증 (서명 확인)
  │                               │    서버 저장 불필요!
```

장점:
- **서버가 상태를 저장하지 않음** (Stateless) → 서버 확장 용이
- 모바일, 웹 등 다양한 클라이언트에서 동일하게 사용
- REST API의 무상태 원칙에 부합

---

## 2. JWT (JSON Web Token)

### 2.1 JWT 구조

JWT는 `.`으로 구분된 3개의 부분으로 구성된다:

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJraW0iLCJleHAiOjE3MDcyMDAwMDB9.abc123signature
└──────── Header ───────┘└──────────── Payload ───────────────────┘└── Signature ──┘
```

#### Header (헤더)

```json
{
  "alg": "HS256",     // 서명 알고리즘
  "typ": "JWT"        // 토큰 타입
}
```

#### Payload (페이로드)

```json
{
  "sub": "kim",           // Subject (사용자 식별자)
  "role": "USER",         // 커스텀 클레임 (역할)
  "iat": 1707100000,      // Issued At (발급 시간)
  "exp": 1707200000       // Expiration (만료 시간)
}
```

> **주의**: Payload는 **Base64 인코딩**이지 암호화가 아니다!
> 누구나 디코딩할 수 있으므로 비밀번호 같은 민감정보를 넣으면 안 된다.

#### Signature (서명)

```
HMACSHA256(
  base64Encode(header) + "." + base64Encode(payload),
  "서버만-아는-비밀키"
)
```

서명의 역할: 토큰이 **위변조되지 않았음**을 검증.
비밀키를 모르면 서명을 만들 수 없으므로, 서버만 유효한 토큰을 발급할 수 있다.

### 2.2 Access Token과 Refresh Token

```
┌──────────────────────────────────────────────────────┐
│  Access Token                                        │
│  - 짧은 수명 (15분 ~ 1시간)                           │
│  - API 요청 시 매번 사용                              │
│  - 탈취되어도 피해 기간이 짧음                        │
├──────────────────────────────────────────────────────┤
│  Refresh Token                                       │
│  - 긴 수명 (7일 ~ 30일)                               │
│  - Access Token 만료 시 재발급에 사용                  │
│  - 서버 DB에 저장하여 탈취 시 무효화 가능              │
└──────────────────────────────────────────────────────┘
```

플로우:

```
1) 로그인 → Access Token + Refresh Token 발급
2) API 요청 → Access Token 사용
3) Access Token 만료 → Refresh Token으로 새 Access Token 발급
4) Refresh Token 만료 → 다시 로그인
```

---

## 3. Spring Security 기초

### 3.1 Security Filter Chain

Spring Security는 **필터 체인**으로 동작한다:

```
HTTP Request
     │
     ▼
┌────────────────────┐
│ Security Filter 1  │ ← CORS 처리
├────────────────────┤
│ Security Filter 2  │ ← CSRF 처리
├────────────────────┤
│ JWT Auth Filter    │ ← JWT 토큰 검증 (커스텀)
├────────────────────┤
│ Authorization      │ ← 권한 확인
│ Filter             │
├────────────────────┤
│ ...                │
└────────┬───────────┘
         │ 모든 필터 통과!
         ▼
┌────────────────────┐
│    Controller      │
└────────────────────┘
```

### 3.2 SecurityConfig 기본 설정

```kotlin
@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }            // REST API이므로 CSRF 비활성화
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)  // 세션 사용 안 함
            }
            .authorizeHttpRequests {
                it.requestMatchers("/api/auth/**").permitAll()  // 로그인/회원가입은 인증 불필요
                it.requestMatchers("/h2-console/**").permitAll()
                it.anyRequest().authenticated()                 // 나머지는 인증 필요
            }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()  // 비밀번호 암호화
    }
}
```

### 3.3 회원가입/로그인 API 설계

```
POST /api/auth/signup    회원가입
  Request:  { "username": "kim", "password": "1234", "email": "kim@test.com" }
  Response: { "id": 1, "username": "kim" }

POST /api/auth/login     로그인
  Request:  { "username": "kim", "password": "1234" }
  Response: { "accessToken": "eyJ...", "refreshToken": "eyJ..." }
```

---

## 4. Docker 기초

### 4.1 컨테이너 vs 가상머신

```
가상머신 (VM)                     컨테이너 (Docker)
┌─────────┬─────────┐            ┌─────┬─────┬─────┐
│  App A  │  App B  │            │App A│App B│App C│
├─────────┼─────────┤            ├─────┴─────┴─────┤
│ Guest OS│ Guest OS│            │  Docker Engine   │
├─────────┴─────────┤            ├──────────────────┤
│   Hypervisor      │            │    Host OS       │
├───────────────────┤            ├──────────────────┤
│    Host OS        │            │   Hardware       │
├───────────────────┤            └──────────────────┘
│   Hardware        │
└───────────────────┘
 느림, 무거움 (GB)                빠름, 가벼움 (MB)
```

### 4.2 Dockerfile 작성

```dockerfile
# 1단계: 빌드
FROM gradle:8-jdk17 AS builder
WORKDIR /app
COPY . .
RUN gradle bootJar --no-daemon

# 2단계: 실행
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 4.3 Docker 명령어

```bash
# 이미지 빌드
docker build -t server-practice .

# 컨테이너 실행
docker run -p 8080:8080 server-practice

# 백그라운드 실행
docker run -d -p 8080:8080 server-practice

# 실행 중인 컨테이너 확인
docker ps

# 컨테이너 중지
docker stop <container_id>
```

### 4.4 docker-compose로 DB 연결

실제 배포 시에는 H2 대신 PostgreSQL 같은 DB를 사용한다:

```yaml
# docker-compose.yml
version: '3.8'
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/practice
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: password
    depends_on:
      - db

  db:
    image: postgres:16
    environment:
      POSTGRES_DB: practice
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: password
    ports:
      - "5432:5432"
```

---

## 5. 배포

### 5.1 환경 분리

```yaml
# application.yml (개발)
spring:
  datasource:
    url: jdbc:h2:mem:practicedb
  jpa:
    hibernate:
      ddl-auto: create-drop

# application-prod.yml (운영)
spring:
  datasource:
    url: ${DATABASE_URL}          # 환경변수로 주입
    username: ${DATABASE_USER}
    password: ${DATABASE_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: validate          # 운영에서는 자동 DDL 금지!
  h2:
    console:
      enabled: false              # H2 콘솔 비활성화
```

### 5.2 무료 PaaS 서비스

| 서비스 | 특징 |
|--------|------|
| Railway | GitHub 연동, 자동 배포, 무료 플랜 |
| Render | Dockerfile 지원, 무료 플랜 (슬립 있음) |
| Fly.io | Docker 기반, 무료 플랜 |

### 5.3 CI/CD 개념

```
코드 Push → 자동 빌드 → 자동 테스트 → 자동 배포
  (Git)     (Build)     (Test)       (Deploy)

CI (Continuous Integration): 코드 변경 → 자동 빌드/테스트
CD (Continuous Deployment):  테스트 통과 → 자동 배포
```

대표 도구: GitHub Actions, Jenkins, GitLab CI

**GitHub Actions 예시:**

```yaml
# .github/workflows/deploy.yml
name: Deploy
on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - run: ./gradlew build
      - run: docker build -t myapp .
      # ... 배포 단계
```

---

## 학습 점검

- [ ] 인증과 인가의 차이를 설명할 수 있다
- [ ] 세션 기반과 토큰 기반 인증의 차이를 안다
- [ ] JWT의 구조(Header.Payload.Signature)를 이해한다
- [ ] Spring Security Filter Chain의 동작 원리를 안다
- [ ] Dockerfile을 작성하고 이미지를 빌드할 수 있다
- [ ] 환경별 설정 분리(application-prod.yml)의 필요성을 안다
