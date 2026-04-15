# Phase 1: Hello World & REST 기초

## 이 단계에서 배운 것

HTTP 요청을 받아서 응답을 돌려주는 가장 기본적인 Spring 웹 애플리케이션을 만들었다.

---

## 핵심 개념

### 1. @RestController — "이 클래스가 API 엔드포인트야"

```kotlin
@RestController
class HelloController { ... }
```

Spring에게 "이 클래스의 메서드들이 HTTP 요청을 처리한다"고 알려준다.
반환값을 자동으로 JSON (또는 문자열)으로 변환해서 HTTP 응답 body에 넣어준다.

**프론트엔드 비유**: Next.js의 `app/api/hello/route.ts` 파일을 만드는 것과 유사. 파일이 곧 API 엔드포인트가 되듯, `@RestController` 클래스의 메서드가 엔드포인트가 된다.

### 2. HTTP 요청에서 데이터 받는 3가지 방법

```kotlin
// 1) 쿼리 파라미터: GET /hello/greet?name=홍수혁
@GetMapping("/hello/greet")
fun greet(@RequestParam name: String): String

// 2) Path Variable: GET /hello/홍수혁
@GetMapping("/hello/{name}")
fun greet(@PathVariable name: String): String

// 3) JSON Body: (Phase 2에서 사용)
@PostMapping("/api/posts")
fun create(@RequestBody request: CreatePostRequest)
```

| 방식 | 프론트 비유 | 언제 쓰는가 |
|------|-----------|-----------|
| `@RequestParam` | URL의 `?key=value` | 필터, 검색, 옵션 |
| `@PathVariable` | URL의 `/users/:id` | 특정 리소스 식별 |
| `@RequestBody` | `fetch()`의 body | 데이터 생성/수정 |

### 3. data class — JSON 자동 변환

```kotlin
data class HelloResponse(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
```

Spring의 Jackson 라이브러리가 `data class` → JSON 변환을 자동으로 해준다:
```json
{ "message": "Hello, 홍수혁!", "timestamp": 1713168000000 }
```

**프론트엔드 비유**: 프론트에서 `JSON.stringify(obj)`를 하듯, Spring은 반환 객체를 자동으로 JSON으로 직렬화한다.

---

## 어노테이션 정리

| 어노테이션 | 역할 |
|-----------|------|
| `@RestController` | 이 클래스 = API 엔드포인트 모음 |
| `@GetMapping("/path")` | GET 요청을 이 메서드에 매핑 |
| `@RequestParam` | 쿼리 파라미터 (`?name=값`) 바인딩 |
| `@PathVariable` | URL 경로 변수 (`/{name}`) 바인딩 |

---

## Spring Boot가 해주는 것

직접 작성한 코드는 Controller 클래스와 DTO 하나뿐이다. 나머지는 Spring Boot가 처리:

- 내장 Tomcat 서버 실행 (포트 8080)
- URL → 메서드 매핑
- 반환값 → JSON 직렬화
- 에러 발생 시 기본 에러 응답

**프론트엔드 비유**: Create React App이나 Vite가 webpack/babel 설정 없이 바로 개발할 수 있게 해주듯, Spring Boot가 서버 설정 없이 바로 API를 만들 수 있게 해준다.
