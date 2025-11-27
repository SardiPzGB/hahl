package ru.itmo.hahl.repository

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.itmo.hahl.dto.MyData

@Repository
class HahlRepository(
    private val template: NamedParameterJdbcTemplate
) {

    private val select = """select * from storedata where id = :id"""
    private val insert = """insert into storedata (id, value) values(:id, :value)"""

    fun store(data: MyData) = template.update(
        insert,
        BeanPropertySqlParameterSource(data)
    )

    fun get(key: Int) = template
        .queryForObject(select, MapSqlParameterSource(mapOf("id" to key)))
        { rs, _ -> MyData(rs.getString("data")) }
        ?.result ?: throw Exception("boom!")

}