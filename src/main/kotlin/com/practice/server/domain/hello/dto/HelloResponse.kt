package com.practice.server.domain.hello.dto

data class HelloResponse(
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
