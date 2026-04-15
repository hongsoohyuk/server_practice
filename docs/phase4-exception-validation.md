# Phase 4: 예외 처리 & 유효성 검증

## 이 단계에서 배운 것

"잘못된 요청"과 "존재하지 않는 데이터"를 체계적으로 처리하고, 모든 에러를 통일된 형식으로 응답하는 방법.

---

## 핵심 개념

### 1. 문제 인식 — Phase 3까지 에러 처리의 한계

```
Phase 3의 에러 응답들:
  404 → body 없이 빈 응답
  Spring 기본 에러 → { timestamp, status, error, path }
  다른 에러 → 또 다른 형식
```

프론트엔드에서 이걸 처리하려면?

```typescript
// 😫 응답 형식이 제각각이라 일관된 에러 처리 불가능
try {
  const res = await fetch('/api/posts/999');
  if (!res.ok) {
    const error = await res.json(); // body가 있을 수도, 없을 수도
    // error.message? error.error? error.detail? 뭐가 있는지 모름
  }
}
```

---

### 2. 해결책 1: 통일된 에러 응답 형식 (ErrorResponse)

```kotlin
data class ErrorResponse(
    val status: Int,          // HTTP 상태 코드
    val code: String,         // 에러 코드 (프론트에서 분기 처리용)
    val message: String,      // 사람이 읽을 메시지
    val timestamp: LocalDateTime
)
```

**모든 에러가 같은 형식**으로 오면 프론트에서 이렇게 처리 가능:

```typescript
// 😊 일관된 에러 처리
axios.interceptors.response.use(null, (error) => {
  const { status, code, message } = error.response.data;
  if (code === 'POST_NOT_FOUND') { /* 404 처리 */ }
  if (code === 'VALIDATION_ERROR') { /* 입력값 에러 처리 */ }
  return Promise.reject(error);
});
```

실제 응답 예시:
```json
{ "status": 404, "code": "POST_NOT_FOUND", "message": "ID 999에 해당하는 게시글을 찾을 수 없습니다", "timestamp": "..." }
{ "status": 400, "code": "VALIDATION_ERROR", "message": "title: 제목은 필수입니다", "timestamp": "..." }
```

---

### 3. 해결책 2: @RestControllerAdvice — 전역 예외 처리

**React ErrorBoundary의 서버 버전.**

```
모든 Controller에서 발생하는 예외를 한 곳에서 잡아서 처리한다.

Client → Controller → Service에서 예외 발생!
    ↓
Spring이 GlobalExceptionHandler 호출
    ↓
ErrorResponse로 변환하여 응답
```

```kotlin
@RestControllerAdvice   // 모든 Controller에 적용
class GlobalExceptionHandler {

    @ExceptionHandler(PostNotFoundException::class)    // 이 예외가 발생하면
    fun handlePostNotFound(e: PostNotFoundException)   // 이 메서드가 처리
        : ResponseEntity<ErrorResponse> { ... }

    @ExceptionHandler(MethodArgumentNotValidException::class)  // @Valid 실패 시
    fun handleValidation(e: MethodArgumentNotValidException)
        : ResponseEntity<ErrorResponse> { ... }

    @ExceptionHandler(Exception::class)  // 위에서 안 잡힌 모든 예외 (최후의 안전망)
    fun handleGeneral(e: Exception)
        : ResponseEntity<ErrorResponse> { ... }
}
```

**이점**: Controller에 에러 분기 코드가 없어진다 → 성공 경로만 남아서 읽기 쉬움.

---

### 4. 해결책 3: @Valid — 요청 데이터 유효성 검증

#### 검증 어노테이션 (DTO에 선언)

```kotlin
data class CreatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @field:Size(min = 2, max = 5000, message = "내용은 2자 이상 5000자 이하여야 합니다")
    val content: String
)
```

#### Controller에서 @Valid로 검증 실행

```kotlin
@PostMapping
fun createPost(@Valid @RequestBody request: CreatePostRequest)
//              ^^^^^^ 이것만 추가하면 Spring이 자동 검증
```

#### Kotlin 주의점: @field: 접두사 필수!

```kotlin
// ❌ Java에서는 되지만, Kotlin에서는 동작 안 함
data class CreatePostRequest(
    @NotBlank val title: String    // 생성자 파라미터에 붙음 (필드가 아님)
)

// ✅ Kotlin에서는 @field:를 명시해야 필드에 붙음
data class CreatePostRequest(
    @field:NotBlank val title: String  // 필드에 붙음 → Spring Validation이 인식
)
```

이유: Kotlin의 `val title: String`은 생성자 파라미터 + 프로퍼티 + backing field를 동시에 생성한다. 어노테이션을 어디에 붙일지 명시하지 않으면 기본적으로 생성자 파라미터에 붙는데, Spring Validation은 **필드**의 어노테이션을 읽으므로 `@field:`가 필요하다.

#### 자주 쓰는 검증 어노테이션

| 어노테이션 | 역할 |
|-----------|------|
| `@NotBlank` | null, "", " " 모두 불허 (문자열 전용) |
| `@NotNull` | null 불허 |
| `@Size(min, max)` | 문자열 길이 또는 컬렉션 크기 제한 |
| `@Min`, `@Max` | 숫자 최소/최대값 |
| `@Email` | 이메일 형식 검증 |
| `@Pattern` | 정규식 매칭 |

---

### 5. 커스텀 예외 — null 반환에서 예외 던지기로

Phase 3 → Phase 4에서 Service 반환 패턴이 바뀌었다:

```kotlin
// Phase 3: null 반환 → Controller에서 매번 null 체크
fun getPost(id: Long): PostResponse? {
    val post = postRepository.findById(id).orElse(null) ?: return null
    return PostResponse.from(post)
}

// Phase 4: 예외 던지기 → GlobalExceptionHandler가 처리
fun getPost(id: Long): PostResponse {
    val post = postRepository.findById(id)
        .orElseThrow { PostNotFoundException(id) }
    return PostResponse.from(post)
}
```

**Controller 변화:**

```kotlin
// Phase 3: 매 메서드마다 null 체크 반복
val response = postService.getPost(id)
    ?: return ResponseEntity.notFound().build()
return ResponseEntity.ok(response)

// Phase 4: 성공 경로만 남음
return ResponseEntity.ok(postService.getPost(id))
```

---

## 적용된 소프트웨어 원칙

| 원칙 | 적용 |
|------|------|
| **DRY** (Don't Repeat Yourself) | null 체크를 매 메서드에서 반복 → GlobalExceptionHandler 한 곳으로 |
| **Fail Fast** | 잘못된 입력은 Service 도달 전에 @Valid에서 즉시 거부 |
| **관심사의 분리** | 에러 처리 로직을 비즈니스 코드에서 분리 |
| **일관성** | 모든 에러가 동일한 ErrorResponse 형식 |

---

## 요청/응답 흐름 정리

```
정상 요청:
  Client → Controller(@Valid 통과) → Service → Repository → DB
       ← 200/201 + PostResponse ←

유효성 검증 실패:
  Client → Controller(@Valid 실패!)
       → MethodArgumentNotValidException 발생
       → GlobalExceptionHandler.handleValidation()
       ← 400 + ErrorResponse ←

존재하지 않는 리소스:
  Client → Controller → Service → Repository(없음!)
       → PostNotFoundException 발생
       → GlobalExceptionHandler.handlePostNotFound()
       ← 404 + ErrorResponse ←
```

---

## 현재 파일 구조

```
domain/post/
├── controller/PostController.kt     ← @Valid 추가, null 체크 제거
├── service/PostService.kt           ← null 대신 예외 던짐
├── repository/PostRepository.kt
├── entity/Post.kt
└── dto/
    ├── CreatePostRequest.kt          ← @NotBlank, @Size 추가
    ├── UpdatePostRequest.kt          ← @NotBlank, @Size 추가
    └── PostResponse.kt

global/exception/
├── ErrorResponse.kt                  ← 통일 에러 응답 형식 (NEW)
├── GlobalExceptionHandler.kt         ← @RestControllerAdvice (NEW)
└── PostNotFoundException.kt          ← 커스텀 예외 (NEW)
```
