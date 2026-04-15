package com.practice.server.domain.post.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

// CreatePostRequest와 동일한 검증 규칙 적용.
// 별도 클래스인 이유: 추후 수정 시에만 다른 규칙이 필요할 수 있음.

data class UpdatePostRequest(
    @field:NotBlank(message = "제목은 필수입니다")
    val title: String,

    @field:NotBlank(message = "내용은 필수입니다")
    @field:Size(min = 2, max = 5000, message = "내용은 2자 이상 5000자 이하여야 합니다")
    val content: String
)
