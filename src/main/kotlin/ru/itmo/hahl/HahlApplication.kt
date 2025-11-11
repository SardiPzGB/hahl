package ru.itmo.hahl

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class HahlApplication

fun main(args: Array<String>) {
	runApplication<HahlApplication>(*args)
}
