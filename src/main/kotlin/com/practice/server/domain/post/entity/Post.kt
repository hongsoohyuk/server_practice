package com.practice.server.domain.post.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
class Post(
    @Column(nullable = false)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
