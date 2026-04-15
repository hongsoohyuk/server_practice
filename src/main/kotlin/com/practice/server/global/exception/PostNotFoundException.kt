package com.practice.server.global.exception

// ============================================================
// [Phase 4] 커스텀 예외 — null 반환 대신 명시적 예외 던지기
// ============================================================
//
// ■ Phase 3의 패턴:
//     // Service
//     fun getPost(id: Long): PostResponse? {    // null 반환
//         val post = postRepository.findById(id).orElse(null) ?: return null
//     }
//     // Controller
//     val response = postService.getPost(id)
//         ?: return ResponseEntity.notFound().build()  // 매번 null 체크
//
//   문제점:
//     1. 모든 Controller 메서드에서 null 체크를 반복해야 함
//     2. "왜 null인지"에 대한 정보가 없음 (권한 부족? 존재하지 않음?)
//     3. null 체크를 까먹으면 NullPointerException 발생
//
// ■ Phase 4의 패턴:
//     // Service
//     fun getPost(id: Long): PostResponse {    // non-null 반환, 없으면 예외
//         val post = postRepository.findById(id)
//             .orElseThrow { PostNotFoundException(id) }
//     }
//     // Controller — null 체크 제거, 깔끔해짐
//     fun getPost(@PathVariable id: Long) = ResponseEntity.ok(postService.getPost(id))
//
//     // GlobalExceptionHandler가 PostNotFoundException을 잡아서 404 응답 생성
//
// ■ 프론트엔드 비유:
//     if (response.data === null) 체크를 매번 하는 대신,
//     axios interceptor에서 에러 상태 코드를 한 번에 처리하는 것과 같은 원리.
// ============================================================

class PostNotFoundException(id: Long) :
    RuntimeException("ID ${id}에 해당하는 게시글을 찾을 수 없습니다")
