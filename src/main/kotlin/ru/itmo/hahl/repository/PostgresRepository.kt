package ru.itmo.hahl.repository

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import ru.itmo.hahl.dto.MyData

@Repository
class PostgresRepository(
    @Qualifier("master")
    private val writeTemplate: NamedParameterJdbcTemplate,
    @Qualifier("replica")
    private val replicaTemplate: NamedParameterJdbcTemplate,
) {

    private val select = """select * from storedata where id = :id"""
    private val insert = """insert into storedata (id, data) values(:id, :data)"""
    private val clear = """delete from storedata"""

    fun store(key: Int, myData: String) = writeTemplate.update(
        insert,
        mapOf(
            "id" to key,
            "data" to myData,
        ).toMutableMap()
    )

    fun getFromMaster(key: Int) = writeTemplate
        .queryForObject(select, MapSqlParameterSource(mapOf("id" to key)))
        { rs, _ -> MyData(rs.getString("data")) }
        ?.result ?: throw Exception("boom!")

    fun getFromReplica(key: Int) = replicaTemplate
        .queryForObject(select, MapSqlParameterSource(mapOf("id" to key)))
        { rs, _ -> MyData(rs.getString("data")) }
        ?.result ?: throw Exception("boom!")

    fun clear() = writeTemplate.update(clear, emptyMap<String, String>().toMutableMap())

}