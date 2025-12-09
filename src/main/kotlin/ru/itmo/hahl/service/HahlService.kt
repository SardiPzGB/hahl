package ru.itmo.hahl.service

import org.springframework.stereotype.Service
import ru.itmo.hahl.repository.PostgresRepository
import ru.itmo.hahl.repository.RedisRepository

@Service
class HahlService(
    private val postgresRepository: PostgresRepository,
    private val redisRepository: RedisRepository
) {


    fun storeInPostgres(id: Int, data: String) {
        postgresRepository.store(id, "data is $data")
    }

    fun getFromPostgres(key: Int) = postgresRepository.get(key)

    fun storeInRedis(key: Int, value: String) = redisRepository.storeValue("$key", value)

    fun getFromRedis(key: Int) = redisRepository.getValue("$key")

    fun clearPostgres()  = postgresRepository.clear()
}