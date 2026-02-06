# Phase 2 - CRUD API와 데이터베이스

## 학습 목표
- 관계형 데이터베이스의 기본 개념을 이해한다
- ORM과 JPA의 개념을 익힌다
- Spring Data JPA로 CRUD API를 구현한다

---

## 1. 데이터베이스 기초

### 1.1 관계형 데이터베이스 (RDB)

데이터를 **테이블(표)** 형태로 저장하는 데이터베이스. 테이블 간의 관계(Relation)를 정의할 수 있다.

```
posts 테이블
┌────┬───────────────┬──────────┬─────────────────────┐
│ id │ title         │ content  │ created_at          │
├────┼───────────────┼──────────┼─────────────────────┤
│  1 │ 첫 번째 글    │ 안녕하세요│ 2025-02-06 10:00:00 │
│  2 │ 두 번째 글    │ 반갑습니다│ 2025-02-06 11:00:00 │
│  3 │ 세 번째 글    │ 감사합니다│ 2025-02-06 12:00:00 │
└────┴───────────────┴──────────┴─────────────────────┘
  ↑        ↑             ↑              ↑
 PK    Column(열)    Column(열)     Column(열)
       ←──────── Row(행) ──────────→
```

핵심 용어:
- **테이블(Table)**: 데이터를 저장하는 단위 (= 엑셀의 시트)
- **행(Row)**: 하나의 데이터 레코드 (= 게시글 1개)
- **열(Column)**: 데이터의 속성 (= title, content 등)
- **Primary Key(PK)**: 각 행을 고유하게 식별하는 값 (= id)

### 1.2 SQL 기본

```sql
-- 조회
SELECT * FROM posts;
SELECT id, title FROM posts WHERE id = 1;

-- 생성
INSERT INTO posts (title, content, created_at)
VALUES ('새 글', '내용입니다', '2025-02-06 10:00:00');

-- 수정
UPDATE posts SET title = '수정된 제목' WHERE id = 1;

-- 삭제
DELETE FROM posts WHERE id = 1;
```

### 1.3 H2 인메모리 DB

H2는 Java로 작성된 경량 데이터베이스이다.

**왜 개발에 H2를 쓰는가?**
- 별도 설치 불필요 (의존성 추가만 하면 됨)
- 애플리케이션 시작 시 자동 생성, 종료 시 자동 삭제
- 개발/테스트에 아주 편리
- 나중에 MySQL, PostgreSQL 등으로 쉽게 교체 가능

```
┌───────────────────────────────────────┐
│            Spring Boot App            │
│                                       │
│  ┌─────────────┐   ┌───────────────┐ │
│  │ Application │──>│  H2 Database  │ │
│  │   Code      │   │  (메모리 안)   │ │
│  └─────────────┘   └───────────────┘ │
│                                       │
│  서버가 꺼지면 데이터도 사라짐!        │
└───────────────────────────────────────┘
```

---

## 2. ORM과 JPA

### 2.1 ORM이 필요한 이유

ORM(Object-Relational Mapping)은 **객체와 테이블을 자동으로 매핑**해주는 기술이다.

**ORM 없이 (JDBC 직접 사용):**

```kotlin
// SQL을 문자열로 직접 작성해야 한다
fun findById(id: Long): Post {
    val sql = "SELECT id, title, content, created_at FROM posts WHERE id = ?"
    val rs = jdbcTemplate.queryForRowSet(sql, id)
    if (rs.next()) {
        return Post(
            id = rs.getLong("id"),
            title = rs.getString("title"),
            content = rs.getString("content"),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime()
        )
    }
    throw NoSuchElementException("Post not found")
}
```

**ORM 사용 (JPA):**

```kotlin
// SQL 없이 메서드 호출만으로 가능
fun findById(id: Long): Post {
    return postRepository.findById(id)
        .orElseThrow { NoSuchElementException("Post not found") }
}
```

### 2.2 JPA와 Hibernate

```
┌─────────────────────────────────────────────────┐
│              JPA (인터페이스/표준)                │
│  Java Persistence API                           │
│  "이런 기능이 있어야 한다"는 스펙                 │
├─────────────────────────────────────────────────┤
│           Hibernate (구현체)                     │
│  JPA 스펙을 실제로 구현한 라이브러리              │
│  SQL 자동 생성, 캐싱, Lazy Loading 등 제공       │
├─────────────────────────────────────────────────┤
│         Spring Data JPA (추상화)                 │
│  JPA를 더 편하게 사용하기 위한 Spring 모듈        │
│  Repository 인터페이스만 정의하면 구현은 자동!     │
└─────────────────────────────────────────────────┘
```

### 2.3 @Entity - JPA 엔티티

데이터베이스 테이블과 매핑되는 클래스:

```kotlin
@Entity
@Table(name = "posts")    // 테이블 이름 (생략하면 클래스명 사용)
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

어노테이션 설명:
- `@Entity`: 이 클래스가 JPA 엔티티임을 선언
- `@Id`: Primary Key 필드
- `@GeneratedValue(IDENTITY)`: DB가 자동으로 ID 생성 (AUTO_INCREMENT)
- `@Column`: 컬럼 속성 설정 (nullable, 길이 등)

### 2.4 Kotlin에서 JPA 엔티티 주의점

#### data class를 쓰면 안 되는 이유

```kotlin
// ❌ BAD - data class 사용 금지
data class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var title: String,
    var content: String
)
```

문제점:
1. `equals()`/`hashCode()`가 모든 필드를 비교 → Lazy Loading된 연관 엔티티에서 문제
2. `toString()`이 모든 필드를 출력 → 양방향 관계에서 무한 재귀
3. JPA는 프록시 객체를 사용하는데, data class의 `copy()`와 충돌

```kotlin
// ✅ GOOD - 일반 class 사용
@Entity
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    var title: String,
    var content: String,
    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

#### allOpen 플러그인

JPA는 엔티티 클래스를 상속해서 프록시를 만든다. 그런데 Kotlin 클래스는 기본적으로 `final`이라 상속이 불가능하다.

`build.gradle.kts`에 이미 설정되어 있다:

```kotlin
// @Entity, @MappedSuperclass, @Embeddable이 붙은 클래스를 open으로 만든다
allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}
```

---

## 3. Spring Data JPA

### 3.1 JpaRepository의 마법

인터페이스만 정의하면 Spring이 구현체를 자동 생성한다:

```kotlin
interface PostRepository : JpaRepository<Post, Long>
// 끝! 이것만으로 CRUD 메서드가 모두 제공된다
```

```
┌──────────────────────────────────────────────┐
│         JpaRepository<Post, Long>            │
│                                              │
│  save(entity)      → INSERT 또는 UPDATE      │
│  findById(id)      → SELECT WHERE id = ?     │
│  findAll()         → SELECT * FROM posts     │
│  deleteById(id)    → DELETE WHERE id = ?     │
│  count()           → SELECT COUNT(*)         │
│  existsById(id)    → SELECT EXISTS(...)      │
└──────────────────────────────────────────────┘
         ▲
         │ Spring이 자동으로 구현체 생성
         │
┌──────────────────────────────────────────────┐
│    SimpleJpaRepository (자동 생성)            │
│    우리가 직접 만들 필요 없음!                 │
└──────────────────────────────────────────────┘
```

### 3.2 Query Method

메서드 이름을 규칙에 맞게 작성하면 Spring이 자동으로 쿼리를 생성한다:

```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    // SELECT * FROM posts WHERE title = ?
    fun findByTitle(title: String): List<Post>

    // SELECT * FROM posts WHERE title LIKE '%keyword%'
    fun findByTitleContaining(keyword: String): List<Post>

    // SELECT * FROM posts WHERE created_at BETWEEN ? AND ?
    fun findByCreatedAtBetween(start: LocalDateTime, end: LocalDateTime): List<Post>

    // SELECT * FROM posts ORDER BY created_at DESC
    fun findAllByOrderByCreatedAtDesc(): List<Post>
}
```

---

## 4. RESTful CRUD 설계

### 4.1 CRUD ↔ HTTP 메서드 매핑

| CRUD | HTTP | URL | 설명 |
|------|------|-----|------|
| Create | POST | `/api/posts` | 게시글 생성 |
| Read (목록) | GET | `/api/posts` | 게시글 목록 조회 |
| Read (단건) | GET | `/api/posts/{id}` | 게시글 단건 조회 |
| Update | PUT | `/api/posts/{id}` | 게시글 수정 |
| Delete | DELETE | `/api/posts/{id}` | 게시글 삭제 |

### 4.2 각 API 요청/응답 예시

#### POST /api/posts - 생성

```http
POST /api/posts HTTP/1.1
Content-Type: application/json

{
  "title": "Spring Boot 학습",
  "content": "재미있어요!"
}
```

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": 1,
  "title": "Spring Boot 학습",
  "content": "재미있어요!",
  "createdAt": "2025-02-06T10:00:00"
}
```

#### GET /api/posts - 목록 조회

```http
GET /api/posts HTTP/1.1
```

```http
HTTP/1.1 200 OK
Content-Type: application/json

[
  {
    "id": 1,
    "title": "Spring Boot 학습",
    "content": "재미있어요!",
    "createdAt": "2025-02-06T10:00:00"
  },
  {
    "id": 2,
    "title": "JPA 학습",
    "content": "신기해요!",
    "createdAt": "2025-02-06T11:00:00"
  }
]
```

#### GET /api/posts/{id} - 단건 조회

```http
GET /api/posts/1 HTTP/1.1
```

```http
HTTP/1.1 200 OK

{
  "id": 1,
  "title": "Spring Boot 학습",
  "content": "재미있어요!",
  "createdAt": "2025-02-06T10:00:00"
}
```

#### PUT /api/posts/{id} - 수정

```http
PUT /api/posts/1 HTTP/1.1
Content-Type: application/json

{
  "title": "수정된 제목",
  "content": "수정된 내용"
}
```

```http
HTTP/1.1 200 OK

{
  "id": 1,
  "title": "수정된 제목",
  "content": "수정된 내용",
  "createdAt": "2025-02-06T10:00:00"
}
```

#### DELETE /api/posts/{id} - 삭제

```http
DELETE /api/posts/1 HTTP/1.1
```

```http
HTTP/1.1 204 No Content
```

### 4.3 @RequestBody와 ResponseEntity

```kotlin
@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postRepository: PostRepository
) {
    // @RequestBody: HTTP Body의 JSON을 객체로 변환
    @PostMapping
    fun createPost(@RequestBody request: CreatePostRequest): ResponseEntity<Post> {
        val post = Post(title = request.title, content = request.content)
        val saved = postRepository.save(post)
        // ResponseEntity로 상태코드 제어 (201 Created)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved)
    }

    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): ResponseEntity<Post> {
        val post = postRepository.findById(id)
        return if (post.isPresent) {
            ResponseEntity.ok(post.get())           // 200 OK
        } else {
            ResponseEntity.notFound().build()       // 404 Not Found
        }
    }

    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Unit> {
        postRepository.deleteById(id)
        return ResponseEntity.noContent().build()   // 204 No Content
    }
}

data class CreatePostRequest(
    val title: String,
    val content: String
)
```

---

## 5. 실습 가이드

### 5.1 Post 엔티티 만들기

고려할 점:
- `id`: Long, 자동 생성
- `title`: 필수, 최대 100자
- `content`: 필수, 긴 텍스트
- `createdAt`: 생성 시점 자동 설정
- 일반 class 사용 (data class X)

### 5.2 curl로 CRUD 테스트 전체 시나리오

```bash
# 1. 게시글 생성
curl -X POST http://localhost:8080/api/posts \
  -H "Content-Type: application/json" \
  -d '{"title":"첫 번째 글","content":"안녕하세요!"}'

# 2. 게시글 목록 조회
curl http://localhost:8080/api/posts

# 3. 게시글 단건 조회
curl http://localhost:8080/api/posts/1

# 4. 게시글 수정
curl -X PUT http://localhost:8080/api/posts/1 \
  -H "Content-Type: application/json" \
  -d '{"title":"수정된 제목","content":"수정된 내용"}'

# 5. 게시글 삭제
curl -X DELETE http://localhost:8080/api/posts/1

# 6. 삭제 확인 (404 예상)
curl -i http://localhost:8080/api/posts/1
```

---

## 학습 점검

- [ ] RDB의 테이블, 행, 열, PK 개념을 안다
- [ ] ORM이 필요한 이유를 설명할 수 있다
- [ ] @Entity, @Id, @GeneratedValue 역할을 안다
- [ ] JpaRepository의 기본 메서드를 사용할 수 있다
- [ ] CRUD 각각에 적절한 HTTP 메서드와 상태코드를 매핑할 수 있다
- [ ] @RequestBody와 ResponseEntity를 사용할 수 있다
