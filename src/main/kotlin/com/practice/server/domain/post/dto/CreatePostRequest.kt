package com.practice.server.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// ============================================================
// [Phase 4] @Valid 유효성 검증 추가
// ============================================================
//
// ■ Phase 3까지:
//     data class CreatePostRequest(val title: String, val content: String)
//     → 빈 문자열("")이든 공백(" ")이든 그냥 통과, DB에 저장됨
//
// ■ Phase 4:
//     @field:NotBlank, @field:Size 등으로 규칙 선언
//     → Controller에서 @Valid와 함께 사용하면 Spring이 자동 검증
//     → 검증 실패 시 MethodArgumentNotValidException 발생
//     → GlobalExceptionHandler가 잡아서 400 Bad Request 응답
//
// ■ Kotlin 주의사항 — @field: 접두사
//     Kotlin의 data class에서 어노테이션을 그냥 붙이면
//     생성자 파라미터에 붙는다 (not 필드). Spring Validation은
//     필드의 어노테이션을 읽으므로 @field:를 명시해야 동작한다.
//
//     Java에서는 필요 없는, Kotlin에서만 주의할 점!
//
// ■ 프론트엔드 비유:
//     React Hook Form이나 Zod로 폼 입력값을 검증하는 것과 같다.
//     클라이언트 검증 + 서버 검증을 모두 하는 것이 좋은 관행.
//     (클라이언트 검증은 UX, 서버 검증은 보안)
// ============================================================

data class CreatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @field:Size(min = 2, max = 5000, message = "내용은 2자 이상 5000자 이하여야 합니다")
    val content: String
)
