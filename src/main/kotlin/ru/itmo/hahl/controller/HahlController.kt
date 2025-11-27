package ru.itmo.hahl.controller

import io.micrometer.core.annotation.Timed
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import ru.itmo.hahl.service.HahlService
import kotlin.random.Random

@Controller
@RequestMapping("api/v1/hahl")
class HahlController(
    private val service: HahlService
) {

    private var count: Int = 0

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

    @GetMapping("/clear")
    fun clear(): ResponseEntity<String> {
        service.clearPostgres()
        return ResponseEntity(
            "cleared",
                HttpStatusCode.valueOf(200)
        )
    }

    @Timed("db_no_cache")
    @GetMapping("/nocached")
    fun getNoCache(@RequestParam id: Int): ResponseEntity<String>  {
        return ResponseEntity(
            service.getFromPostgres(id),
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("db_cached")
    @GetMapping("/cached")
    fun getCached(@RequestParam id: Int): ResponseEntity<String>  {
        val fromRedis: String? = service.getFromRedis(id)
        val result = if (fromRedis == null) {
            println("---- cache miss for [$id]!!")
            val fromPostgres = service.getFromPostgres(id)
            service.storeInRedis(id, fromPostgres)
            fromPostgres
        } else {
            println("---- cache hit for [$id]!!")
            fromRedis
        }
        return ResponseEntity(
            result,
            HttpStatusCode.valueOf(200),
        )
    }

    @Timed("store")
    @GetMapping("/store")
    fun store(@RequestParam data: String): ResponseEntity<String> {
        service.storeInPostgres(count, data)
        count++
        return ResponseEntity(
            "stored $data with index $count",
            HttpStatusCode.valueOf(200),
        )
    }
}