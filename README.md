# Server Practice - Kotlin + Spring Boot 학습 프로젝트

백엔드 개발 입문을 위한 Kotlin + Spring Boot 학습 프로젝트입니다.
단계별로 기능을 추가하며 백엔드 개발의 핵심 개념을 익힙니다.

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Kotlin 1.9 |
| Framework | Spring Boot 3.5 |
| Build Tool | Gradle (Kotlin DSL) |
| Database | H2 (in-memory, 개발용) |
| ORM | Spring Data JPA / Hibernate |
| Java | JDK 17 |

## 시작하기

### 사전 요구사항

- JDK 17 이상 (`java -version`으로 확인)

macOS에서 Homebrew로 설치한 경우:
```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
```

### 실행

```bash
./gradlew bootRun
```

서버가 시작되면 http://localhost:8080 으로 접속할 수 있습니다.

### 빌드 & 테스트

```bash
# 전체 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "com.practice.server.SomeTestClass"

# 특정 테스트 메서드 실행
./gradlew test --tests "com.practice.server.SomeTestClass.someMethod"
```

### H2 Database Console

서버 실행 중 http://localhost:8080/h2-console 에서 DB를 직접 조회할 수 있습니다.
- JDBC URL: `jdbc:h2:mem:practicedb`
- Username: `sa`
- Password: (비워둠)

---

## 학습 로드맵

각 단계를 순서대로 진행하며, 이전 단계에서 만든 코드 위에 기능을 추가합니다.

### Phase 1: Hello World & REST 기초

**목표**: HTTP 요청/응답의 기본 구조를 이해한다.

- [x] `GET /hello` - "Hello, World!" 문자열 반환
- [x] `GET /hello?name=홍수혁` - 쿼리 파라미터 받아서 "Hello, 홍수혁!" 반환
- [x] `GET /hello/{name}` - Path Variable 사용
- [x] JSON 응답 반환 (`data class` 활용)

**학습 키워드**: `@RestController`, `@GetMapping`, `@RequestParam`, `@PathVariable`, `data class`

### Phase 2: CRUD API 만들기

**목표**: RESTful API의 기본 패턴을 익힌다. 게시글(Post) 도메인으로 CRUD를 구현한다.

- [ ] Post 엔티티 생성 (`id`, `title`, `content`, `createdAt`)
- [ ] `POST /api/posts` - 게시글 생성
- [ ] `GET /api/posts` - 게시글 목록 조회
- [ ] `GET /api/posts/{id}` - 게시글 단건 조회
- [ ] `PUT /api/posts/{id}` - 게시글 수정
- [ ] `DELETE /api/posts/{id}` - 게시글 삭제

**학습 키워드**: `@Entity`, `@Id`, `JpaRepository`, `@RequestBody`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, HTTP 상태 코드 (200, 201, 404)

### Phase 3: 계층 분리 (Layered Architecture)

**목표**: Controller - Service - Repository 3계층 구조를 이해한다.

- [ ] `PostController` → `PostService` → `PostRepository` 분리
- [ ] Request DTO / Response DTO 분리 (`CreatePostRequest`, `PostResponse`)
- [ ] Service에서 비즈니스 로직 처리 (엔티티 ↔ DTO 변환)

**학습 키워드**: `@Service`, `@Repository`, DTO 패턴, 관심사의 분리, 의존성 주입(`@Autowired` vs 생성자 주입)

### Phase 4: 예외 처리 & 유효성 검증

**목표**: 에러를 체계적으로 처리하는 방법을 배운다.

- [ ] 존재하지 않는 게시글 조회 시 404 응답
- [ ] `@Valid`를 사용한 요청 데이터 검증 (제목 필수, 내용 최소 길이 등)
- [ ] `@ExceptionHandler` / `@RestControllerAdvice`로 전역 예외 처리
- [ ] 통일된 에러 응답 형식 정의 (`ErrorResponse`)

**학습 키워드**: `@Valid`, `@NotBlank`, `@Size`, `@RestControllerAdvice`, `@ExceptionHandler`, `ResponseEntity`

### Phase 5: 테스트 작성

**목표**: 자동화된 테스트로 코드의 정확성을 검증하는 방법을 배운다.

- [ ] Service 단위 테스트 (JUnit 5 + Mockk)
- [ ] Repository 테스트 (`@DataJpaTest`)
- [ ] Controller 통합 테스트 (`@SpringBootTest` + `MockMvc` 또는 `WebTestClient`)

**학습 키워드**: `@Test`, `@SpringBootTest`, `@DataJpaTest`, `MockMvc`, `Mockk`, Arrange-Act-Assert 패턴

### Phase 6: 연관관계 & 페이징

**목표**: JPA 연관관계와 대량 데이터 처리를 배운다.

- [ ] Comment 엔티티 추가 (Post와 1:N 관계)
- [ ] `POST /api/posts/{postId}/comments` - 댓글 작성
- [ ] `GET /api/posts/{postId}/comments` - 댓글 목록 조회
- [ ] 게시글 목록 페이징 처리 (`Pageable`)
- [ ] 제목으로 검색 기능

**학습 키워드**: `@OneToMany`, `@ManyToOne`, `@JoinColumn`, `Pageable`, `Page<T>`, JPQL / Query Method

### Phase 7 (선택): 인증 & 배포

**목표**: 실제 서비스에 필요한 인증과 배포를 경험한다.

- [ ] Spring Security + JWT 기반 로그인/회원가입
- [ ] 게시글 작성자 정보 연결
- [ ] Docker 이미지 빌드
- [ ] 클라우드 배포 (Railway / Render / AWS)

---

## 프로젝트 구조 (목표)

Phase 3 이후 완성되는 패키지 구조:

```
src/main/kotlin/com/practice/server/
├── ServerPracticeApplication.kt          # 진입점
├── domain/
│   └── post/
│       ├── controller/PostController.kt  # HTTP 요청 처리
│       ├── service/PostService.kt        # 비즈니스 로직
│       ├── repository/PostRepository.kt  # DB 접근
│       ├── entity/Post.kt               # JPA 엔티티
│       └── dto/                          # 요청/응답 DTO
│           ├── CreatePostRequest.kt
│           ├── UpdatePostRequest.kt
│           └── PostResponse.kt
└── global/
    └── exception/                        # 전역 예외 처리
        ├── GlobalExceptionHandler.kt
        └── ErrorResponse.kt
```
