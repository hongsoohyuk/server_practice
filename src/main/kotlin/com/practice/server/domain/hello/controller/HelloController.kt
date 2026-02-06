package com.practice.server.domain.hello.controller

import com.practice.server.domain.hello.dto.HelloResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class HelloController {

    // 1) 기본 문자열 반환
    @GetMapping("/hello")
    fun hello(): String {
        return "Hello, World!"
    }

    // 2) 쿼리 파라미터로 이름 받기 (?name=홍수혁)
    @GetMapping("/hello/greet")
    fun greetWithParam(@RequestParam name: String): String {
        return "Hello, $name!"
    }

    // 3) Path Variable로 이름 받기 (/hello/홍수혁)
    @GetMapping("/hello/{name}")
    fun greetWithPath(@PathVariable name: String): String {
        return "Hello, $name!"
    }

    // 4) JSON 응답 반환 (data class → 자동으로 JSON 변환)
    @GetMapping("/hello/json")
    fun helloJson(@RequestParam(defaultValue = "World") name: String): HelloResponse {
        return HelloResponse(message = "Hello, $name!")
    }
}
