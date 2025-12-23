package ru.itmo.hahl.configuration

import io.debezium.config.Configuration
import io.debezium.embedded.Connect
import io.debezium.engine.ChangeEvent
import io.debezium.engine.DebeziumEngine
import io.debezium.engine.RecordChangeEvent
import io.debezium.engine.format.ChangeEventFormat
import io.debezium.engine.format.Json
import org.springframework.context.annotation.Bean
import ru.itmo.hahl.service.ChangeEventHandler
import org.springframework.context.annotation.Configuration as SpringConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@SpringConfig
class DebeziumConfig {

    @Bean
    fun debeziumConnector(): Configuration {
        return Configuration.create()
            .with("connector.class", "io.debezium.connector.postgresql.PostgresConnector")
            .with("offset.storage", "org.apache.kafka.connect.storage.FileOffsetBackingStore")
            .with("offset.storage.file.filename", "offset.dat")
            .with("offset.flush.interval.ms", 60000)
            .with("name", "postgres-connector")
            .with("database.hostname", "localhost")
            .with("database.port", 5432)
            .with("database.user", "postgres")
            .with("database.password", "password")
            .with("database.dbname", "mydb")
            .with("database.server.name", "postgres-server")
            .with("table.include.list", "public.users,public.orders")
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