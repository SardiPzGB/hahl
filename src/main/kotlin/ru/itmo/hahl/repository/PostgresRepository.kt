package ru.itmo.hahl.repository

import org.springframework.jdbc.core.RowCallbackHandler
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.itmo.hahl.dto.MyData

@Repository
class PostgresRepository(
    private val template: NamedParameterJdbcTemplate
) {

    private val select = """select * from storedata where id = :id"""
    private val insert = """insert into storedata (id, data) values(:id, :data)"""
    private val clear = """delete from storedata"""

    fun store(key: Int, myData: String) = template.update(
        insert,
        mapOf(
            "id" to key,
            "data" to myData,
        ).toMutableMap()
    )

    fun get(key: Int) = template
        .queryForObject(select, MapSqlParameterSource(mapOf("id" to key)))
        { rs, _ -> MyData(rs.getString("data")) }
        ?.result ?: throw Exception("boom!")

    fun clear() = template.update(clear, emptyMap<String, String>().toMutableMap())

}