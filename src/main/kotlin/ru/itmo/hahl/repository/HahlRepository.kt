package ru.itmo.hahl.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class HahlRepository(
    private val template: NamedParameterJdbcTemplate
) {

    fun store() {

    }
}