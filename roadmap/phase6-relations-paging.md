# Phase 6 - JPA 연관관계와 페이징

## 학습 목표
- JPA 연관관계(1:N, N:1)의 개념과 사용법을 익힌다
- N+1 문제를 이해하고 해결법을 안다
- 페이징과 검색 기능을 구현한다

---

## 1. JPA 연관관계 기초

### 1.1 관계의 종류

```
1:1 (일대일)    사용자 ── 프로필    (한 사용자는 하나의 프로필)
1:N (일대다)    게시글 ──< 댓글     (한 게시글에 여러 댓글)
N:M (다대다)    게시글 >──< 태그    (한 게시글에 여러 태그, 한 태그에 여러 게시글)
```

게시글(Post)과 댓글(Comment)의 관계:

```
posts 테이블                       comments 테이블
┌────┬──────────┐                 ┌────┬─────────┬─────────┐
│ id │ title    │                 │ id │ content │ post_id │
├────┼──────────┤                 ├────┼─────────┼─────────┤
│  1 │ 첫 글    │◄──────────────│  1 │ 좋아요!  │    1    │
│    │          │◄──────────────│  2 │ 감사합니다│    1    │
│  2 │ 두번째 글│◄──────────────│  3 │ 잘 봤어요│    2    │
└────┴──────────┘                 └────┴─────────┴─────────┘
  1쪽 (One)                         N쪽 (Many)
                                   post_id = FK(외래키)
```

### 1.2 @ManyToOne, @OneToMany

```kotlin
// Comment 엔티티 - N쪽 (Many)
@Entity
class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)    // 댓글 N : 게시글 1
    @JoinColumn(name = "post_id")         // FK 컬럼명
    val post: Post,

    val createdAt: LocalDateTime = LocalDateTime.now()
)

// Post 엔티티 - 1쪽 (One)
@Entity
class Post(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    var title: String,
    var content: String,

    @OneToMany(mappedBy = "post")          // 게시글 1 : 댓글 N
    val comments: MutableList<Comment> = mutableListOf(),

    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### 1.3 연관관계의 주인 (Owning Side)

DB에서 외래키(FK)를 가진 쪽이 **연관관계의 주인**이다.

```
Comment (주인)     ←→     Post (비주인)
  @ManyToOne               @OneToMany(mappedBy = "post")
  @JoinColumn              FK 관리 안 함 (읽기 전용)
  post_id FK 관리
```

규칙:
- **주인** (N쪽, `@ManyToOne`): 외래키를 직접 관리. `@JoinColumn`으로 FK 지정.
- **비주인** (1쪽, `@OneToMany`): `mappedBy`로 주인을 가리킴. 읽기 전용.

### 1.4 양방향 vs 단방향

```kotlin
// 단방향 (Comment → Post만 참조)
@Entity
class Comment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post          // Comment에서 Post 접근 가능
)

@Entity
class Post(
    // comments 필드 없음 → Post에서 Comment 접근 불가
)
```

```kotlin
// 양방향 (서로 참조)
@Entity
class Comment(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post
)

@Entity
class Post(
    @OneToMany(mappedBy = "post")
    val comments: MutableList<Comment> = mutableListOf()  // Post에서도 Comment 접근 가능
)
```

> 실무 팁: **단방향**으로 시작하고, 필요할 때만 양방향을 추가하자.
> 양방향은 순환참조, 복잡성 증가 등의 단점이 있다.

---

## 2. N+1 문제

### 2.1 N+1 문제란?

게시글 10개를 조회하면서 각 게시글의 댓글도 함께 가져올 때:

```
1번 쿼리: SELECT * FROM posts              (게시글 10개 조회)
2번 쿼리: SELECT * FROM comments WHERE post_id = 1  (1번 게시글의 댓글)
3번 쿼리: SELECT * FROM comments WHERE post_id = 2  (2번 게시글의 댓글)
4번 쿼리: SELECT * FROM comments WHERE post_id = 3  (3번 게시글의 댓글)
...
11번 쿼리: SELECT * FROM comments WHERE post_id = 10 (10번 게시글의 댓글)

총 1 + 10 = 11번 쿼리 실행! (N+1)
```

게시글이 1000개면? **1001번** 쿼리가 실행된다.

### 2.2 Lazy Loading vs Eager Loading

```kotlin
// LAZY (지연 로딩) - 기본값, 권장
@ManyToOne(fetch = FetchType.LAZY)
val post: Post
// → post.title에 접근할 때 비로소 SELECT 실행

// EAGER (즉시 로딩) - 비권장
@ManyToOne(fetch = FetchType.EAGER)
val post: Post
// → Comment를 조회할 때 Post도 항상 함께 조회
```

> `@ManyToOne`의 기본값은 EAGER이다.
> **반드시 LAZY로 변경**해야 N+1 문제를 제어할 수 있다.

### 2.3 해결법: Fetch Join

JPQL의 `JOIN FETCH`로 한 번의 쿼리로 가져온다:

```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p JOIN FETCH p.comments WHERE p.id = :id")
    fun findByIdWithComments(@Param("id") id: Long): Post?
}
```

```sql
-- 실행되는 SQL (1번만!)
SELECT p.*, c.*
FROM posts p
LEFT JOIN comments c ON p.id = c.post_id
WHERE p.id = ?
```

### 2.4 해결법: @EntityGraph

어노테이션으로 더 간단하게:

```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    @EntityGraph(attributePaths = ["comments"])
    fun findWithCommentsById(id: Long): Post?
}
```

---

## 3. 페이징 (Pagination)

### 3.1 왜 페이징이 필요한가?

게시글이 10,000건이면 `findAll()`로 한 번에 가져오는 것은:
- 메모리 부족 위험
- 네트워크 부하
- 클라이언트 렌더링 느림

→ **한 페이지에 20건씩** 나눠서 가져온다.

### 3.2 Spring Data의 Pageable

```kotlin
// Controller
@GetMapping
fun getPosts(
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int
): Page<PostResponse> {
    val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
    return postService.getPosts(pageable)
}

// Service
fun getPosts(pageable: Pageable): Page<PostResponse> {
    return postRepository.findAll(pageable)
        .map { PostResponse.from(it) }
}
```

### 3.3 Page vs Slice

| | Page<T> | Slice<T> |
|---|---|---|
| 전체 개수 | O (COUNT 쿼리 추가 실행) | X |
| 전체 페이지 수 | O | X |
| 용도 | "총 152건 중 1-20" | "더보기" 버튼 |
| 성능 | COUNT 쿼리 비용 | 더 빠름 |

### 3.4 페이징 API 요청/응답

**요청:**

```http
GET /api/posts?page=0&size=20&sort=createdAt,desc
```

**응답:**

```json
{
  "content": [
    { "id": 20, "title": "최신 글", "createdAt": "2025-02-06T12:00" },
    { "id": 19, "title": "이전 글", "createdAt": "2025-02-06T11:00" }
  ],
  "totalElements": 152,
  "totalPages": 8,
  "number": 0,
  "size": 20,
  "first": true,
  "last": false
}
```

---

## 4. 검색 (Query Methods & JPQL)

### 4.1 Query Method 네이밍 규칙

메서드 이름만으로 쿼리를 자동 생성:

```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    // WHERE title = ?
    fun findByTitle(title: String): List<Post>

    // WHERE title LIKE '%keyword%'
    fun findByTitleContaining(keyword: String): List<Post>

    // WHERE title LIKE '%keyword%' (페이징)
    fun findByTitleContaining(keyword: String, pageable: Pageable): Page<Post>

    // WHERE created_at > ?
    fun findByCreatedAtAfter(date: LocalDateTime): List<Post>

    // WHERE title LIKE ? AND content LIKE ?
    fun findByTitleContainingAndContentContaining(
        title: String, content: String
    ): List<Post>

    // ORDER BY created_at DESC
    fun findAllByOrderByCreatedAtDesc(): List<Post>
}
```

주요 키워드:

| 키워드 | 예시 | SQL |
|--------|------|-----|
| Containing | findByTitleContaining("봄") | LIKE '%봄%' |
| StartingWith | findByTitleStartingWith("봄") | LIKE '봄%' |
| EndingWith | findByTitleEndingWith("봄") | LIKE '%봄' |
| Between | findByIdBetween(1, 10) | BETWEEN 1 AND 10 |
| GreaterThan | findByIdGreaterThan(5) | > 5 |
| OrderBy | findAllByOrderByIdDesc() | ORDER BY id DESC |

### 4.2 @Query로 JPQL 직접 작성

복잡한 쿼리는 JPQL로 직접 작성:

```kotlin
interface PostRepository : JpaRepository<Post, Long> {

    // JPQL (엔티티 기반, 테이블 이름이 아닌 클래스 이름 사용)
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword% OR p.content LIKE %:keyword%")
    fun search(@Param("keyword") keyword: String): List<Post>

    // 페이징과 함께
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:keyword%")
    fun searchByTitle(
        @Param("keyword") keyword: String,
        pageable: Pageable
    ): Page<Post>
}
```

### 4.3 검색 + 페이징 API

```kotlin
// Controller
@GetMapping("/search")
fun searchPosts(
    @RequestParam keyword: String,
    @RequestParam(defaultValue = "0") page: Int,
    @RequestParam(defaultValue = "20") size: Int
): Page<PostResponse> {
    val pageable = PageRequest.of(page, size, Sort.by("createdAt").descending())
    return postService.search(keyword, pageable)
}

// Service
fun search(keyword: String, pageable: Pageable): Page<PostResponse> {
    return postRepository.searchByTitle(keyword, pageable)
        .map { PostResponse.from(it) }
}
```

---

## 5. 실습 가이드

### 5.1 Comment 엔티티 설계

```kotlin
@Entity
class Comment(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var content: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    val post: Post,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
```

### 5.2 댓글 API 설계

| 메서드 | URL | 설명 |
|--------|-----|------|
| POST | `/api/posts/{postId}/comments` | 댓글 작성 |
| GET | `/api/posts/{postId}/comments` | 댓글 목록 조회 |
| DELETE | `/api/posts/{postId}/comments/{commentId}` | 댓글 삭제 |

### 5.3 게시글 목록에 페이징/검색 적용

```bash
# 페이징
curl "http://localhost:8080/api/posts?page=0&size=5"

# 검색 + 페이징
curl "http://localhost:8080/api/posts/search?keyword=spring&page=0&size=10"
```

---

## 학습 점검

- [ ] @ManyToOne, @OneToMany의 관계와 사용법을 안다
- [ ] 연관관계의 주인(owning side) 개념을 설명할 수 있다
- [ ] N+1 문제가 무엇이고 Fetch Join으로 해결하는 방법을 안다
- [ ] Lazy vs Eager Loading의 차이를 안다
- [ ] Pageable을 사용한 페이징 API를 만들 수 있다
- [ ] Query Method와 @Query JPQL을 사용할 수 있다
