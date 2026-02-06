# Phase 1 - HTTP와 REST API 기초

## 학습 목표
- HTTP 프로토콜의 기본 구조와 동작 원리를 이해한다
- REST API의 개념과 설계 원칙을 학습한다
- Spring MVC가 HTTP 요청을 처리하는 과정을 파악한다
- JSON 직렬화/역직렬화의 개념을 이해한다

---

## 1. HTTP 프로토콜 기초

### 1.1 HTTP란?

HTTP(HyperText Transfer Protocol)는 웹에서 클라이언트와 서버가 데이터를 주고받기 위한 **약속(프로토콜)**이다. 브라우저로 웹사이트에 접속하거나, 모바일 앱이 서버에서 데이터를 가져올 때 모두 HTTP를 사용한다.

```
┌──────────┐                                    ┌──────────┐
│          │    HTTP Request (요청)              │          │
│ Client   │───────────────────────────────────>│  Server  │
│ (브라우저) │                                    │ (Spring) │
│          │    HTTP Response (응답)             │          │
│          │<───────────────────────────────────│          │
└──────────┘                                    └──────────┘
```

### 1.2 HTTP 요청(Request) 구조

```
┌─────────────────────────────────────────────────────┐
│ Request Line (요청 라인)                             │
│ GET /api/posts/123 HTTP/1.1                         │
├─────────────────────────────────────────────────────┤
│ Headers (헤더)                                      │
│ Host: localhost:8080                                │
│ Content-Type: application/json                      │
│ Accept: application/json                            │
├─────────────────────────────────────────────────────┤
│ Body (본문) - POST/PUT 요청에서 사용                │
│ {                                                   │
│   "title": "Spring Boot 배우기",                    │
│   "content": "재미있어요!"                           │
│ }                                                   │
└─────────────────────────────────────────────────────┘
```

**1) Request Line**: `메서드 경로 HTTP버전`
**2) Headers**: 요청에 대한 메타정보 (Content-Type, Authorization 등)
**3) Body**: 실제 전송 데이터. GET에는 보통 없고, POST/PUT에서 사용

### 1.3 HTTP 응답(Response) 구조

```
┌─────────────────────────────────────────────────────┐
│ Status Line                                         │
│ HTTP/1.1 200 OK                                     │
├─────────────────────────────────────────────────────┤
│ Headers                                             │
│ Content-Type: application/json                      │
│ Content-Length: 87                                   │
├─────────────────────────────────────────────────────┤
│ Body                                                │
│ {                                                   │
│   "id": 123,                                        │
│   "title": "Spring Boot 배우기"                     │
│ }                                                   │
└─────────────────────────────────────────────────────┘
```

### 1.4 HTTP 메서드

| 메서드 | 용도 | Body | 멱등성 |
|--------|------|------|--------|
| GET | 데이터 조회 | 없음 | O |
| POST | 데이터 생성 | 있음 | X |
| PUT | 데이터 전체 수정 | 있음 | O |
| PATCH | 데이터 부분 수정 | 있음 | X |
| DELETE | 데이터 삭제 | 없음 | O |

> **멱등성(Idempotency)**: 같은 요청을 여러 번 보내도 결과가 같은 성질.
> POST는 호출할 때마다 새 리소스가 생성되므로 멱등하지 않다.

### 1.5 HTTP 상태 코드

#### 2xx - 성공

| 코드 | 의미 | 사용 예시 |
|------|------|-----------|
| 200 | OK | GET, PUT 성공 |
| 201 | Created | POST로 리소스 생성 성공 |
| 204 | No Content | DELETE 성공, 응답 본문 없음 |

#### 4xx - 클라이언트 오류

| 코드 | 의미 | 사용 예시 |
|------|------|-----------|
| 400 | Bad Request | 요청 형식이 잘못됨 |
| 401 | Unauthorized | 인증 필요 (로그인 안 됨) |
| 403 | Forbidden | 권한 없음 (로그인은 했지만) |
| 404 | Not Found | 리소스를 찾을 수 없음 |
| 409 | Conflict | 중복 데이터 |

#### 5xx - 서버 오류

| 코드 | 의미 |
|------|------|
| 500 | Internal Server Error (서버 내부 오류) |
| 502 | Bad Gateway |
| 503 | Service Unavailable |

### 1.6 URL 구조

```
https://api.example.com:8080/api/posts?page=1&size=10#section1
└─┬──┘ └──────┬───────┘└┬─┘└───┬────┘└──────┬──────┘└───┬────┘
scheme      host     port   path      query string   fragment
```

- **scheme**: 프로토콜 (http, https)
- **host**: 서버 주소
- **port**: 포트 번호 (기본: http=80, https=443)
- **path**: 리소스 경로
- **query string**: 추가 파라미터 (`?`로 시작, `&`로 구분)
- **fragment**: 페이지 내 위치 (서버로 전송 안 됨)

---

## 2. REST API란?

### 2.1 정의

REST(Representational State Transfer)는 웹의 장점을 최대한 활용하는 **API 설계 방식**이다.

핵심 아이디어:
- **Resource**: URL로 식별되는 대상 (게시글, 사용자 등)
- **Representation**: 리소스를 JSON 등으로 표현
- **State Transfer**: HTTP 메서드로 리소스의 상태를 변경

### 2.2 RESTful URL 설계 규칙

#### 규칙 1: URL에는 명사를 사용하라

```
✅ GET    /api/posts          → 게시글 목록
✅ POST   /api/posts          → 게시글 생성
✅ GET    /api/posts/123      → 게시글 조회
✅ PUT    /api/posts/123      → 게시글 수정
✅ DELETE /api/posts/123      → 게시글 삭제

❌ GET    /api/getPosts
❌ POST   /api/createPost
❌ POST   /api/deletePost
```

HTTP 메서드가 이미 동사이므로, URL에는 명사만 사용한다.

#### 규칙 2: 복수형을 사용하라

```
✅ /api/posts
✅ /api/users
❌ /api/post
❌ /api/user
```

#### 규칙 3: 계층 구조를 표현하라

```
GET  /api/posts/123/comments       → 123번 게시글의 댓글 목록
POST /api/posts/123/comments       → 123번 게시글에 댓글 작성
```

#### 규칙 4: 필터링/정렬/페이징은 쿼리 스트링으로

```
GET /api/posts?page=1&size=20
GET /api/posts?sort=createdAt&order=desc
GET /api/posts?search=spring&author=kim
```

---

## 3. Spring MVC 동작 원리

### 3.1 요청 처리 흐름

```
  Client
    │
    │  HTTP Request
    ▼
┌──────────────────┐
│ DispatcherServlet│  ← 모든 요청의 진입점 (프론트 컨트롤러)
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  HandlerMapping  │  ← URL + 메서드로 어떤 Controller를 호출할지 결정
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│   Controller     │  ← 요청 처리 + 결과 반환
└────────┬─────────┘
         │
         ▼
┌──────────────────┐
│  Jackson(JSON)   │  ← 객체 → JSON 변환
└────────┬─────────┘
         │
         ▼
  HTTP Response → Client
```

### 3.2 @RestController vs @Controller

| | @Controller | @RestController |
|---|---|---|
| 반환 | View 이름 (HTML) | 데이터 (JSON) |
| @ResponseBody | 필요 | 자동 적용 |
| 용도 | 전통적인 웹 페이지 | REST API |

`@RestController` = `@Controller` + `@ResponseBody`

### 3.3 주요 어노테이션

#### @GetMapping - GET 요청 처리

```kotlin
@GetMapping("/hello")
fun hello(): String {
    return "Hello, World!"
}
```

#### @RequestParam - 쿼리 파라미터

```kotlin
// GET /hello/greet?name=홍수혁
@GetMapping("/hello/greet")
fun greet(@RequestParam name: String): String {
    return "Hello, $name!"
}

// 기본값 설정
@GetMapping("/hello/greet")
fun greet(@RequestParam(defaultValue = "World") name: String): String {
    return "Hello, $name!"
}
```

#### @PathVariable - 경로 변수

```kotlin
// GET /hello/홍수혁
@GetMapping("/hello/{name}")
fun greet(@PathVariable name: String): String {
    return "Hello, $name!"
}
```

**@RequestParam vs @PathVariable:**

| | @RequestParam | @PathVariable |
|---|---|---|
| 위치 | `?key=value` | `/path/{variable}` |
| 용도 | 필터링, 정렬, 페이징 | 리소스 식별 |
| 필수 여부 | 선택 가능 | 필수 |
| 예시 | `?page=1&size=10` | `/posts/123` |

---

## 4. JSON과 직렬화

### 4.1 JSON이란?

JSON(JavaScript Object Notation)은 데이터를 표현하는 텍스트 형식이다. REST API의 표준 데이터 형식.

```json
{
  "id": 1,
  "title": "Spring Boot 학습",
  "tags": ["spring", "kotlin"],
  "published": true,
  "author": {
    "name": "홍수혁"
  }
}
```

### 4.2 직렬화와 역직렬화

```
┌──────────────┐   직렬화 (Serialization)    ┌──────────────┐
│  Kotlin 객체  │  ──────────────────────>    │  JSON 문자열  │
│  (data class) │                             │              │
│              │  <──────────────────────    │              │
└──────────────┘   역직렬화 (Deserialization) └──────────────┘
```

Spring Boot는 **Jackson** 라이브러리로 자동 변환한다.

### 4.3 Kotlin data class → JSON

```kotlin
data class HelloResponse(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

Controller에서 반환하면:

```kotlin
@GetMapping("/hello/json")
fun helloJson(): HelloResponse {
    return HelloResponse(message = "Hello!")
}
```

자동으로 이렇게 변환된다:

```json
{
  "message": "Hello!",
  "timestamp": 1738800000000
}
```

---

## 5. 실습 가이드

### 5.1 프로젝트의 HelloController 코드 분석

```kotlin
@RestController                    // ← REST API 컨트롤러 선언
class HelloController {

    @GetMapping("/hello")          // ← GET /hello 매핑
    fun hello(): String {
        return "Hello, World!"     // ← 문자열 그대로 반환
    }

    @GetMapping("/hello/greet")    // ← GET /hello/greet?name=xxx
    fun greetWithParam(
        @RequestParam name: String // ← 쿼리 파라미터 받기
    ): String {
        return "Hello, $name!"     // ← Kotlin 문자열 템플릿
    }

    @GetMapping("/hello/{name}")   // ← GET /hello/홍수혁
    fun greetWithPath(
        @PathVariable name: String // ← 경로 변수 받기
    ): String {
        return "Hello, $name!"
    }

    @GetMapping("/hello/json")     // ← GET /hello/json
    fun helloJson(
        @RequestParam(defaultValue = "World") name: String
    ): HelloResponse {             // ← 객체 반환 → 자동 JSON 변환
        return HelloResponse(message = "Hello, $name!")
    }
}
```

### 5.2 curl로 테스트하기

서버를 실행한 후 (`./gradlew bootRun`), 새 터미널을 열어서:

```bash
# 1) 기본 Hello World
curl http://localhost:8080/hello
# → Hello, World!

# 2) 쿼리 파라미터
curl "http://localhost:8080/hello/greet?name=홍수혁"
# → Hello, 홍수혁!

# 3) Path Variable
curl http://localhost:8080/hello/홍수혁
# → Hello, 홍수혁!

# 4) JSON 응답
curl http://localhost:8080/hello/json
# → {"message":"Hello, World!","timestamp":1738800000000}

curl "http://localhost:8080/hello/json?name=홍수혁"
# → {"message":"Hello, 홍수혁!","timestamp":1738800000000}

# 응답 헤더도 함께 보기
curl -i http://localhost:8080/hello/json
# HTTP/1.1 200
# Content-Type: application/json
# ...

# 요청/응답 전체 상세 보기
curl -v http://localhost:8080/hello
```

> **팁**: 쿼리 스트링(`?`, `&`)이 있는 URL은 반드시 따옴표로 감싸야 한다!

### 5.3 브라우저에서 테스트

GET 요청은 브라우저 주소창에 직접 입력하면 된다:
- http://localhost:8080/hello
- http://localhost:8080/hello/홍수혁
- http://localhost:8080/hello/json?name=홍수혁

---

## 학습 점검

- [ ] HTTP 요청/응답의 구조(Request Line, Headers, Body)를 설명할 수 있다
- [ ] GET, POST, PUT, DELETE의 차이를 안다
- [ ] HTTP 상태 코드 200, 201, 400, 404, 500의 의미를 안다
- [ ] RESTful URL 설계 원칙을 설명할 수 있다
- [ ] @RestController, @GetMapping, @RequestParam, @PathVariable을 사용할 수 있다
- [ ] curl로 API를 테스트할 수 있다
