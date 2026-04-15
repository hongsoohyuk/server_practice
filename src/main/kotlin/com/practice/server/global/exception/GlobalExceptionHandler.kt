package com.practice.server.global.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

// ============================================================
// [Phase 4] 전역 예외 처리 — @RestControllerAdvice
// ============================================================
//
// ■ Phase 3까지의 문제:
//     Controller마다 null 체크, try-catch를 반복하고 있었다:
//       val response = postService.getPost(id)
//           ?: return ResponseEntity.notFound().build()
//
//     이 패턴이 모든 메서드, 모든 Controller에 반복되면?
//     → 에러 처리 로직이 비즈니스 코드 전체에 퍼짐
//
// ■ 해결: @RestControllerAdvice
//     "모든 Controller에서 발생하는 예외를 한 곳에서 잡아서 처리"
//     Service가 예외를 던지면 → 여기서 잡아서 → 통일된 ErrorResponse로 변환
//
// ■ 프론트엔드 비유:
//     React의 ErrorBoundary가 하위 컴포넌트의 에러를 한 곳에서 잡듯이,
//     @RestControllerAdvice가 모든 Controller의 예외를 한 곳에서 잡는다.
//
//     또는 axios interceptor에서 모든 API 에러를 한 곳에서 처리하는 것과 유사.
//       axios.interceptors.response.use(null, (error) => { ... })
//
// ■ 동작 흐름:
//     Client 요청 → Controller → Service에서 예외 발생!
//       → Spring이 자동으로 GlobalExceptionHandler의 해당 @ExceptionHandler 호출
//       → ErrorResponse로 변환하여 클라이언트에 응답
// ============================================================

@RestControllerAdvice
class GlobalExceptionHandler {

    // ── 1) 커스텀 비즈니스 예외 (예: 게시글을 찾을 수 없음) ──

    @ExceptionHandler(PostNotFoundException::class)
    fun handlePostNotFound(e: PostNotFoundException): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.NOT_FOUND.value(),
            code = "POST_NOT_FOUND",
            message = e.message ?: "게시글을 찾을 수 없습니다"
        )
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error)
    }

    // ── 2) @Valid 유효성 검증 실패 ──
    //
    // [변경 전] 빈 제목, 빈 내용을 보내도 그냥 저장됨
    // [변경 후] @Valid가 실패하면 MethodArgumentNotValidException 발생 → 여기서 처리

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        // 모든 필드 에러 메시지를 모아서 하나의 문자열로 조합
        val message = e.bindingResult.fieldErrors
            .joinToString(", ") { "${it.field}: ${it.defaultMessage}" }

        val error = ErrorResponse(
            status = HttpStatus.BAD_REQUEST.value(),
            code = "VALIDATION_ERROR",
            message = message
        )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    // ── 3) 예상하지 못한 모든 예외 (최후의 안전망) ──

    @ExceptionHandler(Exception::class)
    fun handleGeneral(e: Exception): ResponseEntity<ErrorResponse> {
        val error = ErrorResponse(
            status = HttpStatus.INTERNAL_SERVER_ERROR.value(),
            code = "INTERNAL_ERROR",
            message = "서버 내부 오류가 발생했습니다"
            // 실제 에러 메시지(e.message)는 보안상 클라이언트에 노출하지 않음
        )
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}
