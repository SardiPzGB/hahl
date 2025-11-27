package ru.itmo.hahl.repository

import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.namedparam.SqlParameterSource
import org.springframework.stereotype.Repository
import ru.itmo.hahl.dto.MyData

@Repository
class HahlRepository(
    private val template: NamedParameterJdbcTemplate
) {

    private val select = """select * from storedata where id = :id"""
    private val insert = """insert into storedata (id, data) values(:id, :data)"""

    fun store(key: Int, myData: MyData) = template.update(
        insert,
        mapOf(
            "id" to key,
            "data" to myData.result,
        ).toMutableMap()
    )

    fun get(key: Int) = template
        .queryForObject(select, MapSqlParameterSource(mapOf("id" to key)))
        { rs, _ -> MyData(rs.getString("data")) }
        ?.result ?: throw Exception("boom!")

}