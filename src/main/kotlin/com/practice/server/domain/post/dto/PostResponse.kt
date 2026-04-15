package com.practice.server.domain.post.dto

import com.practice.server.domain.post.entity.Post
import java.time.LocalDateTime

data class PostResponse(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(post: Post): PostResponse {
            return PostResponse(
                id = post.id,
                title = post.title,
                content = post.content,
                createdAt = post.createdAt
            )
        }
    }
}
