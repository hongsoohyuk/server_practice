package com.practice.server.domain.post.service

import com.practice.server.domain.post.dto.CreatePostRequest
import com.practice.server.domain.post.dto.PostResponse
import com.practice.server.domain.post.dto.UpdatePostRequest
import com.practice.server.domain.post.entity.Post
import com.practice.server.domain.post.repository.PostRepository
import com.practice.server.global.exception.PostNotFoundException
import org.springframework.stereotype.Service

// ============================================================
// [Phase 4] Service 리팩토링 — null 반환 → 예외 던지기
// ============================================================
//
// ■ 변경 전 (Phase 3):
//     fun getPost(id: Long): PostResponse? {       // nullable 반환
//         val post = ... ?: return null             // 없으면 null
//     }
//     → Controller에서 매번 null 체크 필요
//
// ■ 변경 후 (Phase 4):
//     fun getPost(id: Long): PostResponse {         // non-null 반환
//         val post = ... .orElseThrow { PostNotFoundException(id) }  // 없으면 예외
//     }
//     → Controller가 null을 신경 쓸 필요 없음
//     → GlobalExceptionHandler가 예외를 잡아서 404 응답 생성
//
// ■ 이점: Controller 코드가 깔끔해지고, 에러 처리가 한 곳에 집중됨
// ============================================================

@Service
class PostService(
    private val postRepository: PostRepository
) {

    fun createPost(request: CreatePostRequest): PostResponse {
        val post = Post(
            title = request.title,
            content = request.content
        )
        return PostResponse.from(postRepository.save(post))
    }

    fun getAllPosts(): List<PostResponse> {
        return postRepository.findAll().map { PostResponse.from(it) }
    }

    // [변경] 반환 타입: PostResponse? → PostResponse (non-null)
    // [변경] null 반환 → PostNotFoundException 던짐
    fun getPost(id: Long): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { PostNotFoundException(id) }
        return PostResponse.from(post)
    }

    // [변경] 반환 타입: PostResponse? → PostResponse
    fun updatePost(id: Long, request: UpdatePostRequest): PostResponse {
        val post = postRepository.findById(id)
            .orElseThrow { PostNotFoundException(id) }
        post.title = request.title
        post.content = request.content
        return PostResponse.from(postRepository.save(post))
    }

    // [변경] Boolean 반환 → Unit (없으면 예외)
    fun deletePost(id: Long) {
        if (!postRepository.existsById(id)) {
            throw PostNotFoundException(id)
        }
        postRepository.deleteById(id)
    }
}
