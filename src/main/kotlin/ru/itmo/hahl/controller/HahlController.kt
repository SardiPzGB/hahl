package ru.itmo.hahl.controller

import io.micrometer.core.annotation.Timed
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import ru.itmo.hahl.service.HahlService

@Controller
class HahlController(
    private val service: HahlService
) {

    @Timed
    @GetMapping("/")
    fun helloWorld(): ResponseEntity<String> {
        println("---- hello")
        return ResponseEntity(
            "hello world",
            HttpStatusCode.valueOf(200),
        )
    }
}