package ru.itmo.hahl.repository

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository

@Repository
class RedisRepository(
    private val redisTemplate: StringRedisTemplate
) {

    fun getValue(key: String) = redisTemplate.opsForValue().get(key)

    fun storeValue(key: String, value: String) = redisTemplate.opsForValue().set(key, value)
}