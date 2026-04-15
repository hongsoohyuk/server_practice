package com.practice.server.domain.post.repository

import com.practice.server.domain.post.entity.Post
import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long>
