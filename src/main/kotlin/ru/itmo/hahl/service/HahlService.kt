package ru.itmo.hahl.service

import org.springframework.stereotype.Service
import ru.itmo.hahl.repository.HahlRepository

@Service
class HahlService(
    private val repository: HahlRepository
) {

    fun store() {

    }
}