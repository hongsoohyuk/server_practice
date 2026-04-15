package com.practice.server.global.exception

import java.time.LocalDateTime

// ============================================================
// [Phase 4] 통일된 에러 응답 형식
// ============================================================
//
// ■ Phase 3까지의 에러 응답 문제:
//     - 404 → body 없이 빈 응답
//     - Spring 기본 에러 → { timestamp, status, error, path } 형식
//     - 유효성 검증 실패 → 또 다른 형식
//     → 프론트엔드에서 에러 처리할 때 응답 형식이 제각각이라 파싱이 어려움
//
// ■ 해결: 모든 에러를 동일한 JSON 구조로 반환
//     { "status": 404, "code": "POST_NOT_FOUND", "message": "...", "timestamp": "..." }
//
// ■ 프론트엔드 비유:
//     API 응답을 처리할 때 항상 같은 구조를 기대할 수 있으면
//     error.response.data.message 같은 일관된 에러 핸들링이 가능해진다.
//     → axios interceptor에서 한 번만 처리하면 됨
// ============================================================

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
