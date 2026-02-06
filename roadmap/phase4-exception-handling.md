# Phase 4 - 예외 처리와 유효성 검증

## 학습 목표
- API에서 에러를 체계적으로 처리하는 방법을 이해한다
- Spring의 예외 처리 메커니즘을 활용한다
- Bean Validation으로 입력값을 검증한다

---

## 1. 예외 처리가 중요한 이유

### 1.1 Spring Boot 기본 에러 응답 vs 커스텀 에러 응답

**기본 에러 응답 (불친절):**
```json
{
  "timestamp": "2025-02-06T12:34:56.789+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "path": "/api/posts/999"
}
```

문제: 500(서버 오류)인데 실제로는 "게시글 없음"이고, 메시지도 없다.

**커스텀 에러 응답 (친절):**
```json
{
  "code": "POST_NOT_FOUND",
  "message": "게시글을 찾을 수 없습니다.",
  "detail": "ID 999에 해당하는 게시글이 존재하지 않습니다.",
  "timestamp": "2025-02-06T12:34:56"
}
```

### 1.2 좋은 에러 응답의 조건

1. **적절한 HTTP 상태 코드** - 404, 400, 409 등 상황에 맞는 코드
2. **일관된 형식** - 모든 에러 응답이 같은 구조
3. **명확한 메시지** - 클라이언트가 무엇이 잘못되었는지 알 수 있어야

---

## 2. Spring의 예외 처리 메커니즘

### 2.1 예외 처리 흐름

```
Client Request
      │
      ▼
┌──────────────┐
│  Controller  │ ← 예외 발생!
└──────┬───────┘
       │
       ▼
┌──────────────────────┐
│  @ExceptionHandler   │ ← Controller 내부의 핸들러 (있으면 여기서 처리)
│  (Controller 레벨)   │
└──────┬───────────────┘
       │ 처리 안 되면
       ▼
┌─────────────────────────┐
│ @RestControllerAdvice   │ ← 전역 핸들러 (모든 Controller 공통)
│   (전역 레벨)            │
└──────┬──────────────────┘
       │
       ▼
  Error Response → Client
```

### 2.2 @RestControllerAdvice (전역 예외 처리)

모든 Controller에서 발생하는 예외를 **한 곳에서** 처리한다:

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException::class)
    fun handleNotFound(e: EntityNotFoundException): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = e.errorCode,
            message = e.message ?: "리소스를 찾을 수 없습니다."
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse)
    }

    // 예상하지 못한 모든 예외
    @ExceptionHandler(Exception::class)
    fun handleGeneric(e: Exception): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            code = "INTERNAL_SERVER_ERROR",
            message = "서버 오류가 발생했습니다."
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse)
    }
}
```

### 2.3 ErrorResponse DTO

```kotlin
data class ErrorResponse(
    val code: String,
    val message: String,
    val detail: String? = null,
    val timestamp: String = LocalDateTime.now().toString()
)
```

### 2.4 커스텀 예외 클래스

비즈니스 로직에 맞는 예외를 직접 만든다:

```kotlin
// 기본 비즈니스 예외
open class BusinessException(
    message: String,
    val errorCode: String
) : RuntimeException(message)

// 엔티티를 찾을 수 없을 때
class EntityNotFoundException(
    entityName: String,
    id: Any
) : BusinessException(
    message = "${entityName}(id=$id)를 찾을 수 없습니다.",
    errorCode = "${entityName.uppercase()}_NOT_FOUND"
)
```

**Service에서 사용:**

```kotlin
@Service
class PostService(private val postRepository: PostRepository) {

    fun getPost(id: Long): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Post", id) }
        return PostResponse.from(post)
    }
}
```

---

## 3. Bean Validation (유효성 검증)

### 3.1 왜 서버에서 검증하는가?

> "프론트엔드에서 이미 검증했는데요?"

프론트엔드 검증은 **UX를 위한 것**이다. 악의적인 사용자는 curl, Postman 등으로 API를 직접 호출할 수 있으므로, **서버 검증은 보안을 위해 필수**이다.

```
┌───────────────────────────────┐
│  클라이언트 검증 (프론트엔드)  │ ← UX 향상 (즉각적 피드백)
└───────────┬───────────────────┘
            ▼
     [HTTP Request]
            ▼
┌───────────────────────────────┐
│  서버 검증 (백엔드)           │ ← 보안 (악의적 요청 방어)
└───────────────────────────────┘

두 검증은 상호 보완적!
```

### 3.2 주요 어노테이션

| 어노테이션 | 설명 |
|-----------|------|
| `@NotNull` | null 불가 |
| `@NotBlank` | null, 빈 문자열, 공백만 불가 |
| `@Size(min, max)` | 문자열 길이 / 컬렉션 크기 |
| `@Min` / `@Max` | 숫자 최소/최대 |
| `@Email` | 이메일 형식 |
| `@Pattern(regexp)` | 정규식 |
| `@Positive` | 양수 |
| `@Past` / `@Future` | 과거/미래 날짜 |

### 3.3 DTO에 Validation 적용

```kotlin
data class CreatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다.")
    @field:Size(min = 2, max = 100, message = "제목은 2~100자여야 합니다.")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다.")
    @field:Size(min = 10, message = "내용은 10자 이상이어야 합니다.")
    val content: String
)
```

> **중요**: Kotlin에서는 반드시 `@field:` 접두사를 붙여야 한다!
> Kotlin 프로퍼티는 field + getter + setter를 생성하는데,
> validation은 field에 적용되어야 하기 때문.

### 3.4 Controller에서 @Valid 사용

```kotlin
@PostMapping
fun createPost(
    @Valid @RequestBody request: CreatePostRequest  // ← @Valid 추가
): ResponseEntity<PostResponse> {
    val post = postService.createPost(request)
    return ResponseEntity.status(HttpStatus.CREATED).body(post)
}
```

검증 실패 시 `MethodArgumentNotValidException`이 자동으로 발생한다.

### 3.5 검증 에러 응답 처리

```kotlin
// 검증 전용 에러 응답 형식
data class ValidationErrorResponse(
    val code: String,
    val message: String,
    val errors: List<FieldError>,
    val timestamp: String = LocalDateTime.now().toString()
)

data class FieldError(
    val field: String,           // 어떤 필드에서 에러?
    val message: String,         // 에러 메시지
    val rejectedValue: String?   // 거부된 값
)
```

```kotlin
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val fieldErrors = e.bindingResult.fieldErrors.map { error ->
            FieldError(
                field = error.field,
                message = error.defaultMessage ?: "유효하지 않은 값입니다.",
                rejectedValue = error.rejectedValue?.toString()
            )
        }

        val response = ValidationErrorResponse(
            code = "VALIDATION_FAILED",
            message = "입력값 검증에 실패했습니다.",
            errors = fieldErrors
        )
        return ResponseEntity.badRequest().body(response)
    }
}
```

**응답 예시:**

```json
{
  "code": "VALIDATION_FAILED",
  "message": "입력값 검증에 실패했습니다.",
  "errors": [
    {
      "field": "title",
      "message": "제목은 2~100자여야 합니다.",
      "rejectedValue": "A"
    },
    {
      "field": "content",
      "message": "내용은 10자 이상이어야 합니다.",
      "rejectedValue": "짧음"
    }
  ]
}
```

---

## 4. 실습 가이드

### 4.1 구현 순서

1. `ErrorResponse`, `ValidationErrorResponse` DTO 만들기
2. `BusinessException`, `EntityNotFoundException` 커스텀 예외 만들기
3. `GlobalExceptionHandler` 구현 (@RestControllerAdvice)
4. `CreatePostRequest`, `UpdatePostRequest`에 `@field:NotBlank`, `@field:Size` 추가
5. Controller에 `@Valid` 추가
6. curl로 테스트:
   - 존재하지 않는 게시글 조회 → 404
   - 빈 제목으로 생성 → 400 + 필드별 에러 메시지

---

## 학습 점검

- [ ] @RestControllerAdvice의 역할을 설명할 수 있다
- [ ] 커스텀 예외 클래스를 만들 수 있다
- [ ] 왜 서버에서 유효성 검증이 필요한지 설명할 수 있다
- [ ] Kotlin에서 @field: 접두사가 필요한 이유를 안다
- [ ] MethodArgumentNotValidException을 처리하여 필드별 에러 응답을 만들 수 있다
