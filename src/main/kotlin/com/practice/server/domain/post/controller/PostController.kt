package com.practice.server.domain.post.controller

import com.practice.server.domain.post.dto.CreatePostRequest
import com.practice.server.domain.post.dto.PostResponse
import com.practice.server.domain.post.dto.UpdatePostRequest
import com.practice.server.domain.post.service.PostService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// ============================================================
// [Phase 4] Controller 리팩토링 — null 체크 제거, @Valid 추가
// ============================================================
//
// ■ 변경 전 (Phase 3): Controller가 null 체크를 직접 수행
//     val response = postService.getPost(id)
//         ?: return ResponseEntity.notFound().build()   // 매 메서드마다 반복
//
// ■ 변경 후 (Phase 4): Service가 예외를 던지고, GlobalExceptionHandler가 처리
//     val response = postService.getPost(id)            // null 체크 불필요
//     → 없는 게시글이면 PostNotFoundException 발생
//     → GlobalExceptionHandler가 잡아서 404 + ErrorResponse 반환
//
// ■ 결과: Controller가 "성공 경로(happy path)"만 담당
//     에러 분기가 사라져서 코드가 훨씬 읽기 쉬워짐
//
// ■ @Valid 추가
//     @RequestBody 앞에 @Valid를 붙이면
//     DTO의 @NotBlank, @Size 등의 검증을 Spring이 자동으로 수행.
//     실패 시 MethodArgumentNotValidException → GlobalExceptionHandler 처리
// ============================================================

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val postService: PostService
) {

    // [변경] @Valid 추가 → CreatePostRequest의 검증 어노테이션 작동
    @PostMapping
    fun createPost(@Valid @RequestBody request: CreatePostRequest): ResponseEntity<PostResponse> {
        val response = postService.createPost(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(response)
    }

    @GetMapping
    fun getAllPosts(): ResponseEntity<List<PostResponse>> {
        return ResponseEntity.ok(postService.getAllPosts())
    }

    // [변경] null 체크(?: return notFound) 제거
    // 없는 게시글 → PostNotFoundException → GlobalExceptionHandler가 404 처리
    @GetMapping("/{id}")
    fun getPost(@PathVariable id: Long): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.getPost(id))
    }

    // [변경] @Valid 추가 + null 체크 제거
    @PutMapping("/{id}")
    fun updatePost(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): ResponseEntity<PostResponse> {
        return ResponseEntity.ok(postService.updatePost(id, request))
    }

    // [변경] if (!deleted) 분기 제거
    @DeleteMapping("/{id}")
    fun deletePost(@PathVariable id: Long): ResponseEntity<Void> {
        postService.deletePost(id)
        return ResponseEntity.noContent().build()
    }
}
