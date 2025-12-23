package ru.itmo.hahl.configuration

import io.debezium.config.Configuration
import io.debezium.embedded.Connect
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.RecordChangeEvent
import io.debezium.engine.format.ChangeEventFormat
import io.debezium.engine.format.Json
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import ru.itmo.hahl.service.ChangeEventHandler
import org.springframework.context.annotation.Configuration as SpringConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.sql.DataSource

@SpringConfig
class DebeziumConfig {

    @Value("\${spring.datasource.url}")
    private lateinit var databaseUrl: String

    @Value("\${spring.datasource.hikari.username}")
    private lateinit var username: String

    @Value("\${spring.datasource.hikari.password}")
    private lateinit var password: String

    @Value("\${app.debezium.table}")
    private lateinit var dbTable: String

    @Bean
    fun debeziumConnector(): Configuration {
        val cleanUrl = databaseUrl.substringAfter("//") // "localhost:5432/hahldatabase"
        val hostPort = cleanUrl.substringBefore("/")    // "localhost:5432"
        val dbname = cleanUrl.substringAfter("/")       // "hahldatabase"

        val hostname = hostPort.substringBefore(":")    // "localhost"
        val port = hostPort.substringAfter(":", "5432").toInt()

        return Configuration.create()
            .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
            .with("offset.storage.file.filename", "offset.dat")
            .with("offset.flush.interval.ms", 60000)
            .with("name", "postgres-connector")
            .with("database.hostname", hostname)
            .with("database.port", port)
            .with("database.user", username)
            .with("database.password", password)
            .with("database.dbname", dbname)
            .with("topic.prefix", "hahl_cdc")
            .with("table.include.list", dbTable)
            .with("plugin.name", "pgoutput")
            .with("snapshot.mode", "initial")
            .with("slot.name", "debezium_slot")
            .with("tombstones.on.delete", "false")
            .build()
    }

    @Bean
    fun debeziumEngine(
        connectorConfiguration: Configuration,
        changeEventHandler: ChangeEventHandler
    ): DebeziumEngine<ChangeEvent<String, String>>? {
        return DebeziumEngine.create(Json::class.java)
            .using(connectorConfiguration.asProperties())
            .notifying { record ->
                changeEventHandler.handleChangeEvent(record)
            }.build()
    }

    @Bean(destroyMethod = "shutdown")
    fun debeziumExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor { r ->
            Thread(r, "debezium-engine-thread").apply {
                isDaemon = true
            }
        }
    }
}