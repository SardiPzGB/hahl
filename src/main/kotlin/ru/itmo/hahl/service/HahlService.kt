package ru.itmo.hahl.service

import org.springframework.stereotype.Service
import ru.itmo.hahl.dto.MyData
import ru.itmo.hahl.repository.HahlRepository

@Service
class HahlService(
    private val repository: HahlRepository
) {


    fun store() {
        for ( i: Int in 0.. 100) {
            repository.store(i, MyData("data if $i"))
        }
    }

    fun getFromDb(key: Int) = repository.get(key)
}