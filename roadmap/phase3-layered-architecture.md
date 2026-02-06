# Phase 3 - 계층형 아키텍처와 DTO 패턴

## 학습 목표
- 왜 코드를 계층으로 나누는지 이해한다
- Controller-Service-Repository 3계층 구조를 익힌다
- 의존성 주입(DI)의 개념을 이해한다
- DTO 패턴을 적용한다

---

## 1. 왜 계층을 나누는가?

### 1.1 모든 로직이 Controller에 있을 때

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository
) {
    @PostMapping
    fun createPost(@RequestBody request: CreatePostRequest): ResponseEntity<Post> {
        // 검증 로직
        if (request.title.isBlank()) {
            throw IllegalArgumentException("제목은 필수입니다")
        }
        if (request.content.length < 10) {
            throw IllegalArgumentException("내용은 10자 이상이어야 합니다")
        }

        // 비즈니스 로직
        val post = Post(
            title = request.title.trim(),
            content = request.content
        )

        // DB 저장
        val saved = postRepository.save(post)

        // 응답 변환
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }
}
```

문제점:
1. **Controller가 너무 많은 일을 한다** (검증 + 비즈니스 + DB + 응답 변환)
2. **코드 재사용 불가** - 같은 비즈니스 로직을 다른 곳에서 쓰려면 복사해야 함
3. **테스트 어려움** - HTTP 요청 없이는 비즈니스 로직을 테스트할 수 없음
4. **변경 영향 범위 큼** - DB 변경이 API 응답에 바로 영향

### 1.2 관심사의 분리 (Separation of Concerns)

각 계층이 **하나의 관심사만** 담당하도록 분리한다:

```
"HTTP 요청을 어떻게 받을까?"     → Controller
"비즈니스 규칙이 뭘까?"          → Service
"데이터를 어떻게 저장/조회할까?"  → Repository
```

---

## 2. 3계층 아키텍처

```
┌─────────────────────────────────────────────────────┐
│                    Client (브라우저, 앱)              │
└──────────────────────┬──────────────────────────────┘
                       │ HTTP Request / Response
                       ▼
┌─────────────────────────────────────────────────────┐
│  Controller 계층                                     │
│  - HTTP 요청 파싱 (URL, 파라미터, Body)              │
│  - 요청 검증 (@Valid)                                │
│  - Service 호출                                      │
│  - HTTP 응답 생성 (상태코드, JSON)                   │
│  ✗ 비즈니스 로직 X                                   │
│  ✗ DB 직접 접근 X                                    │
└──────────────────────┬──────────────────────────────┘
                       │ DTO
                       ▼
┌─────────────────────────────────────────────────────┐
│  Service 계층                                        │
│  - 비즈니스 로직 처리                                │
│  - 트랜잭션 관리 (@Transactional)                    │
│  - 여러 Repository 조합                              │
│  - Entity ↔ DTO 변환                                │
│  ✗ HTTP 관련 코드 X                                  │
│  ✗ Controller 의존 X                                 │
└──────────────────────┬──────────────────────────────┘
                       │ Entity
                       ▼
┌─────────────────────────────────────────────────────┐
│  Repository 계층                                     │
│  - 데이터 저장/조회/수정/삭제                         │
│  - JPA 쿼리 실행                                     │
│  ✗ 비즈니스 로직 X                                   │
│  ✗ HTTP 관련 코드 X                                  │
└──────────────────────┬──────────────────────────────┘
                       │ SQL
                       ▼
┌─────────────────────────────────────────────────────┐
│                   Database (H2)                      │
└─────────────────────────────────────────────────────┘
```

**단방향 의존성 규칙**: Controller → Service → Repository (역방향 금지!)

- Controller는 Service를 알지만, Service는 Controller를 모른다
- Service는 Repository를 알지만, Repository는 Service를 모른다

---

## 3. 의존성 주입 (Dependency Injection)

### 3.1 DI가 필요한 이유

#### DI 없이 (직접 생성)

```kotlin
class PostController {
    // Controller가 직접 Service를 생성
    private val postService = PostService(PostRepository())  // ← 문제!

    // PostRepository의 생성자가 바뀌면? PostService가 바뀌면?
    // → Controller도 수정해야 함 (강한 결합)
}
```

#### DI 사용 (외부에서 주입)

```kotlin
class PostController(
    private val postService: PostService  // ← Spring이 알아서 넣어줌
) {
    // Controller는 Service의 생성 방법을 몰라도 됨 (느슨한 결합)
}
```

### 3.2 Spring의 IoC 컨테이너

IoC(Inversion of Control) = 제어의 역전. 객체 생성을 개발자가 아닌 **Spring이 관리**한다.

```
┌─────────────────────────────────────────┐
│          Spring IoC Container           │
│                                         │
│  ┌─────────────┐  ┌─────────────────┐  │
│  │ PostController│  │ PostService    │  │
│  │   (Bean)     │──>│   (Bean)       │  │
│  └─────────────┘  └──────┬──────────┘  │
│                          │              │
│                          ▼              │
│                   ┌─────────────────┐  │
│                   │ PostRepository  │  │
│                   │   (Bean)        │  │
│                   └─────────────────┘  │
│                                         │
│  Spring이 Bean을 생성하고               │
│  의존관계를 자동으로 연결(주입)한다       │
└─────────────────────────────────────────┘
```

Bean으로 등록하는 어노테이션:
- `@Controller` / `@RestController`: 컨트롤러
- `@Service`: 서비스
- `@Repository`: 리포지토리
- `@Component`: 범용

### 3.3 생성자 주입 (Kotlin 권장 방식)

```kotlin
// ✅ 생성자 주입 (Kotlin에서 가장 자연스럽다)
@Service
class PostService(
    private val postRepository: PostRepository  // 생성자 파라미터 = 주입 대상
)

// ❌ 필드 주입 (@Autowired) - 권장하지 않음
@Service
class PostService {
    @Autowired
    private lateinit var postRepository: PostRepository
    // 문제: 테스트 시 Mock 교체가 어려움
    // 문제: 불변성 보장 불가 (var)
}
```

Kotlin에서 생성자 주입이 좋은 이유:
- `val`로 불변 보장
- 컴파일 시점에 의존성 누락 감지
- 테스트에서 Mock 객체 쉽게 전달 가능

---

## 4. DTO 패턴

### 4.1 Entity를 직접 노출하면 안 되는 이유

```kotlin
// ❌ Entity를 그대로 반환
@GetMapping("/{id}")
fun getPost(@PathVariable id: Long): Post {
    return postRepository.findById(id).get()
}
```

문제점:

**1) 보안** - 노출하면 안 되는 필드가 포함될 수 있다

```kotlin
@Entity
class User(
    val id: Long,
    val username: String,
    val password: String,    // ← 비밀번호가 응답에 포함됨!
    val email: String
)
```

**2) API 스펙 변경** - Entity 필드를 변경하면 API 응답도 바뀐다

```kotlin
// Entity에 필드 추가 → API 응답이 의도치 않게 변경됨
@Entity
class Post(
    ...
    val internalMemo: String  // 내부 메모인데 API에 노출됨
)
```

**3) 순환참조** - 양방향 관계에서 무한 루프

```kotlin
@Entity class Post(
    @OneToMany val comments: List<Comment>  // Post → Comment
)
@Entity class Comment(
    @ManyToOne val post: Post               // Comment → Post
)
// JSON 변환 시: Post → comments → post → comments → post → ...무한!
```

### 4.2 Request DTO vs Response DTO

```
Client                                  Server

         ──── Request DTO ──────>
CreatePostRequest                     PostService
{                                        │
  "title": "제목",                       │ Entity로 변환
  "content": "내용"                      │ DB 저장
}                                        │
                                         │
         <─── Response DTO ──────
PostResponse                          PostService
{                                        │
  "id": 1,                              │ Entity에서 변환
  "title": "제목",                       │
  "content": "내용",
  "createdAt": "2025-..."
}
```

### 4.3 코드 예시

```kotlin
// Request DTO (클라이언트 → 서버)
data class CreatePostRequest(
    val title: String,
    val content: String
)

data class UpdatePostRequest(
    val title: String,
    val content: String
)

// Response DTO (서버 → 클라이언트)
data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime
) {
    companion object {
        // Entity → DTO 변환 팩토리 메서드
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                title = post.title,
                content = post.content,
                createdAt = post.createdAt
            )
        }
    }
}
```

> DTO에는 `data class`를 사용한다. DTO는 단순한 데이터 운반 객체이므로
> `equals()`, `hashCode()`, `copy()` 등이 유용하다.
> (Entity에만 일반 class를 사용하는 것)

---

## 5. 실습 가이드

### 5.1 리팩토링 단계

Phase 2에서 만든 코드를 3계층으로 분리하는 과정:

**Before (Controller에 모든 로직):**

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository
) {
    @PostMapping
    fun create(@RequestBody request: CreatePostRequest): ResponseEntity<Post> {
        val post = Post(title = request.title, content = request.content)
        val saved = postRepository.save(post)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }
}
```

**After (3계층 분리):**

```kotlin
// Controller - HTTP 처리만
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {
    @PostMapping
    fun create(@RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val response = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}

// Service - 비즈니스 로직
@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository
) {
    @Transactional
    fun createPost(request: CreatePostRequest): PostResponse {
        val post = Post(
            title = request.title,
            content = request.content
        )
        val saved = postRepository.save(post)
        return PostResponse.from(saved)
    }

    fun getPost(id: Long): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { NoSuchElementException("게시글(id=$id)을 찾을 수 없습니다") }
        return PostResponse.from(post)
    }

    fun getAllPosts(): List<PostResponse> {
        return postRepository.findAll().map { PostResponse.from(it) }
    }

    @Transactional
    fun updatePost(id: Long, request: UpdatePostRequest): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { NoSuchElementException("게시글(id=$id)을 찾을 수 없습니다") }
        post.title = request.title
        post.content = request.content
        return PostResponse.from(post)  // 변경 감지(dirty checking)로 자동 UPDATE
    }

    @Transactional
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw NoSuchElementException("게시글(id=$id)을 찾을 수 없습니다")
        }
        postRepository.deleteById(id)
    }
}

// Repository - DB 접근
interface PostRepository : JpaRepository<Post, Long>
```

### 5.2 패키지 구조

```
src/main/kotlin/com/practice/server/
├── ServerPracticeApplication.kt
└── domain/
    └── post/
        ├── controller/
        │   └── PostController.kt
        ├── service/
        │   └── PostService.kt
        ├── repository/
        │   └── PostRepository.kt
        ├── entity/
        │   └── Post.kt
        └── dto/
            ├── CreatePostRequest.kt
            ├── UpdatePostRequest.kt
            └── PostResponse.kt
```

---

## 학습 점검

- [ ] Controller, Service, Repository 각 계층의 역할을 설명할 수 있다
- [ ] 왜 계층을 나누는지 이유를 3가지 이상 말할 수 있다
- [ ] 의존성 주입(DI)이 무엇인지 설명할 수 있다
- [ ] Kotlin에서 생성자 주입이 왜 권장되는지 안다
- [ ] Entity를 API 응답에 직접 사용하면 안 되는 이유를 안다
- [ ] Request DTO와 Response DTO의 역할 차이를 안다
