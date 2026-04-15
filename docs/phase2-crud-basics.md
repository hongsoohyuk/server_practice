# Phase 2: CRUD API 기초

## 이 단계에서 배운 것

REST API의 기본 패턴인 CRUD(Create, Read, Update, Delete)를 구현했다.

---

## 핵심 개념

### 1. JPA Entity — 데이터베이스 테이블과 1:1 매핑되는 클래스

```kotlin
@Entity
class Post(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

**프론트엔드 비유**: TypeScript에서 API 응답의 타입을 `interface`로 정의하는 것과 유사하지만, Entity는 실제 DB 테이블 구조를 결정한다.

| 어노테이션 | 역할 |
|-----------|------|
| `@Entity` | "이 클래스는 DB 테이블이야" |
| `@Id` | 이 필드가 Primary Key |
| `@GeneratedValue` | ID를 자동 생성 (auto increment) |
| `@Column` | 컬럼 세부 설정 (nullable, 타입 등) |

**왜 `data class`가 아닌 일반 `class`인가?**

JPA는 내부적으로 Entity의 프록시 객체를 만든다 (지연 로딩 등). `data class`는 `equals()`, `hashCode()`, `copy()`를 자동 생성하는데, 이것이 JPA 프록시와 충돌할 수 있다. 그래서 Entity는 일반 `class`로 만드는 것이 관례.

### 2. JpaRepository — SQL을 안 써도 되는 DB 접근 인터페이스

```kotlin
interface PostRepository : JpaRepository<Post, Long>
```

이 한 줄만 작성하면 Spring Data JPA가 아래 메서드를 자동으로 구현해준다:

| 메서드 | SQL 대응 |
|--------|---------|
| `save(entity)` | INSERT 또는 UPDATE |
| `findById(id)` | SELECT * WHERE id = ? |
| `findAll()` | SELECT * |
| `deleteById(id)` | DELETE WHERE id = ? |
| `existsById(id)` | SELECT COUNT(*) WHERE id = ? |

**프론트엔드 비유**: React Query나 SWR이 API 호출 보일러플레이트를 줄여주는 것처럼, JpaRepository가 SQL 보일러플레이트를 없애준다.

### 3. HTTP 메서드와 REST 규칙

| 동작 | HTTP 메서드 | URL | 응답 코드 |
|------|-----------|-----|----------|
| 생성 | POST | `/api/posts` | 201 Created |
| 목록 조회 | GET | `/api/posts` | 200 OK |
| 단건 조회 | GET | `/api/posts/{id}` | 200 OK / 404 |
| 수정 | PUT | `/api/posts/{id}` | 200 OK / 404 |
| 삭제 | DELETE | `/api/posts/{id}` | 204 No Content / 404 |

**왜 204 No Content?**: 삭제는 돌려줄 데이터가 없으므로 body 없이 "성공했다"만 알려준다.

### 4. ResponseEntity — HTTP 응답을 세밀하게 제어

```kotlin
// 상태 코드 + body 함께 반환
ResponseEntity.status(HttpStatus.CREATED).body(data)  // 201 + JSON
ResponseEntity.ok(data)                                // 200 + JSON
ResponseEntity.notFound().build()                      // 404, body 없음
ResponseEntity.noContent().build()                     // 204, body 없음
```

**프론트엔드 비유**: `fetch()` 응답에서 `response.status`와 `response.json()`을 분리해서 보내는 쪽의 입장이라고 생각하면 된다.

---

## Phase 2의 한계 (Phase 3에서 개선)

이 단계의 Controller는 모든 것을 혼자 한다:

```kotlin
// Phase 2: Controller가 Repository를 직접 사용
class PostController(private val postRepository: PostRepository) {
    fun createPost(@RequestBody request: Map<String, String>) {
        val post = Post(title = request["title"] ?: "", ...)  // Entity 직접 생성
        postRepository.save(post)                              // DB 직접 접근
    }
}
```

문제점:
1. **`Map<String, String>`** — 어떤 필드가 필요한지 코드를 읽어야 알 수 있음 (타입 안전성 없음)
2. **Controller에 비즈니스 로직** — HTTP 처리와 데이터 로직이 섞여 있어 테스트/유지보수 어려움
3. **계층 없음** — 나중에 "게시글 작성 시 알림 발송" 같은 로직을 어디에 넣을지 애매함

→ Phase 3에서 Service 계층과 Request DTO를 도입해 해결한다.
