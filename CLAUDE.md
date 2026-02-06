# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Kotlin + Spring Boot 백엔드 학습 프로젝트. 게시글(Post) CRUD API를 단계별로 구현하며 백엔드 개발을 익히는 것이 목표.

## Tech Stack

- Kotlin 1.9, Spring Boot 3.5, Gradle (Kotlin DSL)
- Spring Data JPA + H2 (in-memory DB)
- Spring Validation (`jakarta.validation`)
- JDK 17

## Build & Run Commands

```bash
# JAVA_HOME 설정 필요 (macOS Homebrew)
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home

./gradlew build          # 빌드 + 테스트
./gradlew bootRun        # 서버 실행 (localhost:8080)
./gradlew test           # 전체 테스트
./gradlew test --tests "com.practice.server.ClassName"          # 특정 클래스 테스트
./gradlew test --tests "com.practice.server.ClassName.method"   # 특정 메서드 테스트
```

## Architecture

도메인별 패키지 구조 (domain-based packaging):

```
com.practice.server
├── domain/{도메인명}/
│   ├── controller/    # @RestController - HTTP 요청/응답 처리
│   ├── service/       # @Service - 비즈니스 로직
│   ├── repository/    # JpaRepository 인터페이스
│   ├── entity/        # @Entity JPA 엔티티
│   └── dto/           # Request/Response DTO
└── global/
    └── exception/     # @RestControllerAdvice 전역 예외 처리
```

## Coding Conventions

- **언어**: 코드는 영어, 주석/문서는 한국어 허용
- **DTO 변환**: Entity를 직접 API 응답으로 노출하지 않음. 반드시 Response DTO로 변환
- **의존성 주입**: `@Autowired` 대신 생성자 주입 사용 (Kotlin에서는 primary constructor)
- **엔티티 설계**: `data class` 대신 일반 `class` 사용 (JPA 프록시 호환성)
- **설정 파일**: `application.yml` 사용 (`application.properties` 아님)

## Database

- H2 in-memory DB (`jdbc:h2:mem:practicedb`)
- H2 Console: http://localhost:8080/h2-console (username: `sa`, password: 없음)
- `ddl-auto: create-drop` - 서버 재시작 시 테이블 재생성됨

## Key Dependencies

- `spring-boot-starter-web`: REST API
- `spring-boot-starter-data-jpa`: JPA/Hibernate ORM
- `spring-boot-starter-validation`: 요청 데이터 유효성 검증
- `h2`: 개발용 in-memory DB
- `spring-boot-devtools`: 코드 변경 시 자동 재시작
