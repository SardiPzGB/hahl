package ru.itmo.hahl.service

import org.springframework.stereotype.Service
import ru.itmo.hahl.dto.MyData
import ru.itmo.hahl.repository.HahlRepository

@Service
class HahlService(
    private val repository: HahlRepository
) {

    fun store(data: String) = repository.store(MyData(data))

    fun getFromDb(key: Int) = repository.get(key)
}