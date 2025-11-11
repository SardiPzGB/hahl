package ru.itmo.hahl.controller

import io.micrometer.core.annotation.Timed
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import ru.itmo.hahl.service.HahlService

@Controller
@RequestMapping("api/v1/hahl")
class HahlController(
    private val service: HahlService
) {

    @Timed("hello_world")
    @GetMapping("/")
    fun helloWorld(): ResponseEntity<String> {
        println("---- hello")
        return ResponseEntity(
            "hello world",
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("store")
    @PostMapping("/store")
    fun store(@RequestBody body: HahlBody): ResponseEntity<String> {
        val result = service.store(body.data)
        return ResponseEntity(
            result,
            HttpStatusCode.valueOf(200),
        )
    }
}

data class HahlBody (
    val data: String
)