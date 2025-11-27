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
import java.time.Duration
import kotlin.random.Random

@Controller
@RequestMapping("api/v1/hahl")
class HahlController(
    private val service: HahlService
) {

    @Timed("hello_world")
    @GetMapping("/hello")
    fun helloWorld(): ResponseEntity<String> {
        println("---- hello")
        return ResponseEntity(
            "hello world",
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("hello_world_laggy")
    @GetMapping("/laggy")
    fun helloWorldLaggy(): ResponseEntity<String> {
        val status = if (Random.nextInt(0, 100) > 94) {
            HttpStatusCode.valueOf(500)
        } else HttpStatusCode.valueOf(200)
        println("---- hello laggy with status [$status]")
        return ResponseEntity(
            "hello world laggy with status [$status]",
            status,
        )
    }

    @Timed("hello_world_sleepy")
    @GetMapping("/sleepy")
    fun helloWorldSleepy(): ResponseEntity<String> {
        val sleep = Random.nextLong(0, 200)
        Thread.sleep(Duration.ofMillis(sleep))
        println("---- hello sleepy, slept for [$sleep] ms")
        return ResponseEntity(
            "hello world sleepy, slept for [$sleep] ms",
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("db_no_cache")
    @GetMapping("/nocache")
    fun getNoCache(): ResponseEntity<String>  {
        val num = Random.nextInt(1, 100)
        return ResponseEntity(
            service.getFromDb(num),
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("store")
    @PostMapping("/store")
    fun store(@RequestBody body: HahlBody): ResponseEntity<String> {
        val result = service.store(body.data)
        return ResponseEntity(
            "$result",
            HttpStatusCode.valueOf(200),
        )
    }
}

data class HahlBody (
    val data: String
)