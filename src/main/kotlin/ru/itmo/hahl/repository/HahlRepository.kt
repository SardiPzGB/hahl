package ru.itmo.hahl.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class HahlRepository(
    private val template: NamedParameterJdbcTemplate
) {

    fun store(data: String): String {

        return UUID.randomUUID().toString()
    }
}