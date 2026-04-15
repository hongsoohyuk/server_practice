# Phase 3: 계층 분리 (Layered Architecture)

## 이 단계에서 배운 것

Phase 2의 "모든 걸 Controller가 하는" 구조를 **Controller → Service → Repository** 3계층으로 분리했다.

---

## 핵심 개념

### 1. 왜 계층을 나누는가?

**한 줄 요약**: 각 클래스가 한 가지 이유로만 변경되게 하기 위해. (Single Responsibility Principle)

**프론트엔드 비유**:

```
React 컴포넌트 구조              Spring 계층 구조
─────────────────              ──────────────────
Page (라우팅, 레이아웃)     ↔    Controller (HTTP 요청/응답)
Custom Hook (상태, 로직)    ↔    Service (비즈니스 로직)
API 함수 (fetch 호출)       ↔    Repository (DB 접근)
```

React에서 컴포넌트 안에 API 호출, 상태 관리, UI 렌더링을 전부 넣으면 유지보수가 어렵듯이, Spring에서도 관심사를 분리한다.

### 2. 각 계층의 책임

```
┌──────────────────────────────────────────────────────┐
│  Controller (@RestController)                        │
│  - HTTP 요청을 받고, 응답을 반환                         │
│  - 상태 코드 결정 (200, 201, 404 등)                    │
│  - Service를 호출하고 결과에 따라 응답 형식 결정             │
│  ⚠️ 비즈니스 로직을 담으면 안 됨                          │
├──────────────────────────────────────────────────────┤
│  Service (@Service)                                  │
│  - 비즈니스 로직 처리                                   │
│  - DTO ↔ Entity 변환                                  │
│  - 트랜잭션 관리 (@Transactional)                       │
│  ⚠️ HTTP 개념(상태 코드, 헤더 등)을 알면 안 됨              │
├──────────────────────────────────────────────────────┤
│  Repository (JpaRepository)                          │
│  - DB CRUD만 담당                                     │
│  - Spring Data JPA가 구현을 자동 생성                     │
│  ⚠️ 비즈니스 규칙을 담으면 안 됨                          │
└──────────────────────────────────────────────────────┘
```

**판단 기준**: "이 코드는 HTTP가 아닌 환경(예: 배치 작업, 메시지 큐)에서도 의미가 있는가?"
- Yes → Service에 둔다
- No (HTTP 상태 코드 결정 등) → Controller에 둔다

### 3. 리팩토링: 무엇이 바뀌었나

#### Controller — 비즈니스 로직 제거

```kotlin
// ❌ Phase 2: Controller가 직접 Entity 생성하고 Repository 호출
class PostController(private val postRepository: PostRepository) {

    fun createPost(@RequestBody request: Map<String, String>): ResponseEntity<PostResponse> {
        val post = Post(
            title = request["title"] ?: "",
            content = request["content"] ?: ""
        )
        val savedPost = postRepository.save(post)
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(savedPost))
    }
}

// ✅ Phase 3: Controller는 Service에 위임만 함
class PostController(private val postService: PostService) {

    fun createPost(@RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val response = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }
}
```

Controller가 더 이상 `Post` Entity와 `PostRepository`를 import하지 않는다.
→ DB 구조가 바뀌어도 Controller는 수정할 필요 없음.

#### Service — 비즈니스 로직 전담

```kotlin
@Service
class PostService(private val postRepository: PostRepository) {

    fun createPost(request: CreatePostRequest): PostResponse {
        val post = Post(title = request.title, content = request.content)
        return PostResponse.from(postRepository.save(post))
    }

    // null 반환 → Controller가 404 처리 (HTTP 결정은 Controller 몫)
    fun getPost(id: Long): PostResponse? {
        val post = postRepository.findById(id).orElse(null) ?: return null
        return PostResponse.from(post)
    }
}
```

### 4. Request DTO — `Map<String, String>` 을 타입 안전한 클래스로 교체

```kotlin
// ❌ Phase 2: 어떤 필드가 필요한지 코드를 읽어야 알 수 있음
fun createPost(@RequestBody request: Map<String, String>)
val title = request["title"] ?: ""   // "titl" 오타 → 런타임에서야 발견

// ✅ Phase 3: API 스펙이 클래스 자체로 문서화됨
fun createPost(@RequestBody request: CreatePostRequest)
// request.title ← 컴파일 시점에 타입 체크
```

```kotlin
data class CreatePostRequest(
    val title: String,
    val content: String
)
```

**프론트엔드 비유**: `any` 타입 대신 TypeScript `interface`를 쓰는 이유와 동일.

---

## 적용된 소프트웨어 원칙

| 원칙 | 적용 내용 |
|------|----------|
| **SRP** (단일 책임) | Controller는 HTTP만, Service는 비즈니스만, Repository는 DB만 |
| **DIP** (의존성 역전) | Controller → Service → Repository 한 방향으로만 의존 |
| **관심사의 분리** | HTTP 관심사와 비즈니스 로직을 물리적으로 다른 클래스에 배치 |

---

## 현재 파일 구조

```
domain/post/
├── controller/
│   └── PostController.kt     ← HTTP 요청/응답만 처리
├── service/
│   └── PostService.kt        ← 비즈니스 로직 (DTO ↔ Entity 변환)
├── repository/
│   └── PostRepository.kt     ← DB 접근 (Spring이 자동 구현)
├── entity/
│   └── Post.kt               ← JPA 엔티티 (DB 테이블 매핑)
└── dto/
    ├── CreatePostRequest.kt   ← 생성 요청 DTO
    ├── UpdatePostRequest.kt   ← 수정 요청 DTO
    └── PostResponse.kt        ← 응답 DTO (Entity 직접 노출 방지)
```

---

## 다음 단계 (Phase 4) 미리보기

현재 한계:
- 빈 제목으로 게시글을 만들 수 있음 → `@Valid` + `@NotBlank`로 유효성 검증 필요
- 에러 응답 형식이 일관성 없음 → `@RestControllerAdvice`로 전역 예외 처리 필요
