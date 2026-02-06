# Phase 5 - 테스트 전략과 작성법

## 학습 목표
- 테스트를 작성하는 이유와 종류를 이해한다
- JUnit 5와 Mockk로 단위 테스트를 작성한다
- @DataJpaTest, @SpringBootTest로 통합 테스트를 작성한다

---

## 1. 테스트를 작성하는 이유

### 1.1 수동 테스트의 한계

curl로 매번 테스트하는 것의 문제:
- 서버를 켜야 함
- 모든 시나리오를 매번 손으로 확인
- API가 늘어나면 검증 시간이 기하급수적으로 증가
- "이전에 잘 되던 기능이 깨졌는지" 확인 불가 (회귀 버그)

### 1.2 테스트 피라미드

```
        /\
       /  \
      / E2E\         ← 느리고 비쌈 (전체 시스템 테스트)
     /──────\
    /  통합   \       ← 중간 (여러 계층 함께 테스트)
   /──────────\
  /   단위     \      ← 빠르고 쌈 (함수/클래스 단위 테스트)
 /──────────────\
```

- **단위 테스트**: 클래스 하나, 메서드 하나를 독립적으로 테스트
- **통합 테스트**: Controller → Service → Repository → DB 흐름을 테스트
- **E2E 테스트**: 실제 브라우저/클라이언트에서 전체 시나리오 테스트

---

## 2. JUnit 5 기초

### 2.1 기본 구조

```kotlin
class PostServiceTest {

    @Test
    @DisplayName("게시글을 정상적으로 생성한다")
    fun createPost() {
        // Arrange (준비)
        val request = CreatePostRequest(title = "제목", content = "내용입니다 10자 이상")

        // Act (실행)
        val result = postService.createPost(request)

        // Assert (검증)
        assertEquals("제목", result.title)
        assertNotNull(result.id)
    }
}
```

### 2.2 주요 어노테이션

| 어노테이션 | 설명 |
|-----------|------|
| `@Test` | 테스트 메서드 표시 |
| `@DisplayName` | 테스트 이름 (한글 가능) |
| `@BeforeEach` | 각 테스트 전에 실행 |
| `@AfterEach` | 각 테스트 후에 실행 |
| `@Disabled` | 테스트 비활성화 |

### 2.3 주요 Assertions

```kotlin
// 값 비교
assertEquals("예상값", actual)
assertNotEquals("이것이 아님", actual)

// null 검사
assertNotNull(result)
assertNull(result)

// 불리언
assertTrue(result.isNotEmpty())
assertFalse(result.isEmpty())

// 예외 검증
assertThrows<EntityNotFoundException> {
    postService.getPost(999L)
}

// 예외 메시지까지 검증
val exception = assertThrows<EntityNotFoundException> {
    postService.getPost(999L)
}
assertEquals("Post(id=999)를 찾을 수 없습니다.", exception.message)
```

### 2.4 Arrange-Act-Assert 패턴

모든 테스트를 3단계로 구성한다:

```kotlin
@Test
fun `게시글 수정 - 존재하지 않는 게시글이면 예외 발생`() {
    // Arrange (준비) - 테스트에 필요한 데이터/상태 설정
    val nonExistentId = 999L
    val request = UpdatePostRequest(title = "새 제목", content = "새 내용 10자 이상")

    // Act & Assert (실행 & 검증) - 예외 테스트는 함께 작성하기도 함
    assertThrows<EntityNotFoundException> {
        postService.updatePost(nonExistentId, request)
    }
}
```

---

## 3. 단위 테스트 (Unit Test)

### 3.1 Mock이 필요한 이유

Service 테스트에서 **진짜 DB를 사용하면 안 된다**:
- 테스트가 느려짐
- DB 상태에 따라 결과가 달라짐
- DB 없는 환경에서 테스트 불가

→ Repository를 **가짜 객체(Mock)**로 교체해서 테스트한다.

```
실제 환경:
PostService → PostRepository → H2 Database

테스트 환경:
PostService → MockPostRepository (가짜)
                  └─ findById(1) 호출하면 미리 정해둔 Post 반환
```

### 3.2 Mockk 라이브러리

Kotlin에서는 **Mockk**가 가장 자연스럽다 (Mockito는 Java 친화적).

`build.gradle.kts`에 의존성 추가:

```kotlin
dependencies {
    testImplementation("io.mockk:mockk:1.13.13")
}
```

### 3.3 기본 사용법

```kotlin
class PostServiceTest {

    private val postRepository = mockk<PostRepository>()
    private val postService = PostService(postRepository)

    @Test
    @DisplayName("게시글 단건 조회 - 성공")
    fun getPost_success() {
        // Arrange - Mock 동작 설정
        val post = Post(id = 1L, title = "제목", content = "내용")
        every { postRepository.findById(1L) } returns Optional.of(post)

        // Act
        val result = postService.getPost(1L)

        // Assert
        assertEquals(1L, result.id)
        assertEquals("제목", result.title)

        // Mock이 올바르게 호출되었는지 검증
        verify(exactly = 1) { postRepository.findById(1L) }
    }

    @Test
    @DisplayName("게시글 단건 조회 - 존재하지 않으면 예외")
    fun getPost_notFound() {
        // Arrange
        every { postRepository.findById(999L) } returns Optional.empty()

        // Act & Assert
        assertThrows<EntityNotFoundException> {
            postService.getPost(999L)
        }
    }

    @Test
    @DisplayName("게시글 생성")
    fun createPost() {
        // Arrange
        val request = CreatePostRequest(title = "제목", content = "내용입니다 10자 이상")
        val savedPost = Post(id = 1L, title = "제목", content = "내용입니다 10자 이상")
        every { postRepository.save(any()) } returns savedPost

        // Act
        val result = postService.createPost(request)

        // Assert
        assertEquals(1L, result.id)
        assertEquals("제목", result.title)
        verify { postRepository.save(any()) }
    }
}
```

### 3.4 Mockk 핵심 함수

```kotlin
// Mock 객체 생성
val mock = mockk<PostRepository>()

// 동작 설정: "이렇게 호출하면 이것을 반환해라"
every { mock.findById(1L) } returns Optional.of(post)
every { mock.save(any()) } returns savedPost
every { mock.deleteById(any()) } just Runs  // void 메서드

// 예외 던지기
every { mock.findById(999L) } throws EntityNotFoundException("Post", 999L)

// 호출 검증
verify { mock.findById(1L) }              // 1번 이상 호출됨
verify(exactly = 1) { mock.save(any()) }  // 정확히 1번
verify(exactly = 0) { mock.deleteById(any()) }  // 호출 안 됨
```

---

## 4. 통합 테스트 (Integration Test)

### 4.1 @DataJpaTest - Repository 테스트

실제 H2 DB를 사용하여 JPA 쿼리를 테스트한다:

```kotlin
@DataJpaTest
class PostRepositoryTest {

    @Autowired
    private lateinit var postRepository: PostRepository

    @Test
    @DisplayName("게시글 저장 및 조회")
    fun saveAndFind() {
        // Arrange
        val post = Post(title = "테스트 제목", content = "테스트 내용")

        // Act
        val saved = postRepository.save(post)
        val found = postRepository.findById(saved.id)

        // Assert
        assertTrue(found.isPresent)
        assertEquals("테스트 제목", found.get().title)
    }

    @Test
    @DisplayName("제목으로 검색")
    fun findByTitleContaining() {
        // Arrange
        postRepository.save(Post(title = "Spring Boot 학습", content = "내용1"))
        postRepository.save(Post(title = "Kotlin 학습", content = "내용2"))
        postRepository.save(Post(title = "React 학습", content = "내용3"))

        // Act
        val results = postRepository.findByTitleContaining("학습")

        // Assert
        assertEquals(3, results.size)
    }
}
```

### 4.2 @SpringBootTest + MockMvc - Controller 테스트

전체 Spring 컨텍스트를 띄워서 API를 테스트한다:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    @DisplayName("POST /api/posts - 게시글 생성 성공")
    fun createPost() {
        val request = CreatePostRequest(
            title = "테스트 제목",
            content = "테스트 내용 10자 이상입니다"
        )

        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.title").value("테스트 제목"))
            .andExpect(jsonPath("$.id").exists())
    }

    @Test
    @DisplayName("POST /api/posts - 제목 누락시 400")
    fun createPost_validation_fail() {
        val request = mapOf("title" to "", "content" to "내용 10자 이상입니다")

        mockMvc.perform(
            post("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
    }

    @Test
    @DisplayName("GET /api/posts/{id} - 존재하지 않는 게시글 조회시 404")
    fun getPost_notFound() {
        mockMvc.perform(get("/api/posts/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
    }
}
```

### 4.3 MockMvc 주요 메서드

```kotlin
// HTTP 요청 만들기
mockMvc.perform(
    get("/api/posts")                    // GET 요청
    post("/api/posts")                   // POST 요청
    put("/api/posts/1")                  // PUT 요청
    delete("/api/posts/1")               // DELETE 요청
)

// 요청 설정
    .contentType(MediaType.APPLICATION_JSON)  // Content-Type 헤더
    .content(jsonString)                      // 요청 본문

// 응답 검증 (andExpect 체이닝)
    .andExpect(status().isOk)                    // 200
    .andExpect(status().isCreated)               // 201
    .andExpect(status().isNotFound)              // 404
    .andExpect(status().isBadRequest)            // 400
    .andExpect(jsonPath("$.id").value(1))         // JSON 필드 값
    .andExpect(jsonPath("$.title").exists())       // 필드 존재 여부
    .andExpect(jsonPath("$.items.length()").value(3))  // 배열 크기
```

---

## 5. 실습 가이드

### 5.1 PostService 테스트 시나리오

- 게시글 생성 → 정상 반환
- 게시글 단건 조회 → 성공
- 게시글 단건 조회 → 존재하지 않으면 예외
- 게시글 목록 조회 → 전체 반환
- 게시글 수정 → 성공
- 게시글 수정 → 존재하지 않으면 예외
- 게시글 삭제 → 성공
- 게시글 삭제 → 존재하지 않으면 예외

### 5.2 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 특정 클래스
./gradlew test --tests "com.practice.server.domain.post.service.PostServiceTest"

# 특정 메서드
./gradlew test --tests "*.PostServiceTest.getPost_success"
```

---

## 학습 점검

- [ ] 단위 테스트와 통합 테스트의 차이를 안다
- [ ] Mock이 필요한 이유를 설명할 수 있다
- [ ] Mockk의 every, verify를 사용할 수 있다
- [ ] @DataJpaTest와 @SpringBootTest의 차이를 안다
- [ ] MockMvc로 API 테스트를 작성할 수 있다
