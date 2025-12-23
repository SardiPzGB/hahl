package ru.itmo.hahl.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.debezium.engine.ChangeEvent
import io.micrometer.core.instrument.MeterRegistry
import mu.KLogging
import org.springframework.stereotype.Component
import ru.itmo.hahl.metrics.DebeziumMetrics
import java.time.Duration
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.math.log

@Component
class ChangeEventHandler(
    private val objectMapper: ObjectMapper,
    private val metrics: DebeziumMetrics,
    private val registry: MeterRegistry
) {

    companion object : KLogging()

    private val eventSizeHistogram = metrics.getEventSizeHistogram()

    fun handleChangeEvent(changeEvent: ChangeEvent<String, String>) {
        val startTime = Instant.now()

        try {
            val key = changeEvent.key()
            val value = changeEvent.value()

            // Измеряем размер события
            val eventSize = (key?.length ?: 0) + (value?.length ?: 0)
            eventSizeHistogram.record(eventSize.toDouble())

            logger.debug { "Processing event of size: $eventSize bytes" }

            if (value != null) {
                logger.info("--------------------------------------------------")
                logger.info("get not null value [$value]")

                logger.info("--------------------------------------------------")
                val eventData = objectMapper.readTree(value)
                logger.info("get not null eventData [$eventData]")

                logger.info("--------------------------------------------------")
                val payload = eventData.get("payload")
                logger.info("get not null payload [$payload]")

                logger.info("--------------------------------------------------")
                val operation = payload?.get("op")?.asText() ?: "null operation"
                logger.info("get not null operation [$operation]")

                logger.info("--------------------------------------------------")
                val source = payload.get("source")
                logger.info("get not null source [$source]")

                logger.info("--------------------------------------------------")
                val table = source.get("table").asText()
                logger.info("get not null table [$table]")

                logger.info("--------------------------------------------------")
                val tsMs = payload?.get("ts_ms")?.asLong() ?: 0L
                logger.info("get not null tsMs [$tsMs]")

                // Вычисляем лаг репликации
                val replicationLag = System.currentTimeMillis() - tsMs
                metrics.setReplicationLag("postgres-connector", replicationLag)

                // Регистрируем событие
                registry.counter("debezium.events",
                    "operation", operation,
                    "table", table
                ).increment()

                // Обрабатываем операцию
                when (operation) {
                    "c" -> {
                        metrics.incrementInsert(table)
                        handleCreate(payload, table)
                    }
                    "u" -> {
                        metrics.incrementUpdate(table)
                        handleUpdate(payload, table)
                    }
                    "d" -> {
                        metrics.incrementDelete(table)
                        handleDelete(payload, table)
                    }
                    "r" -> {
                        metrics.incrementRead(table)
                        handleRead(payload, table)
                    }
                    else -> {
                        logger.warn { "Unknown operation: $operation" }
                        registry.counter("debezium.events.unknown").increment()
                    }
                }

                // Замеряем время обработки
                val processingTime = Duration.between(startTime, Instant.now()).toMillis()
                metrics.recordProcessingTime(processingTime, TimeUnit.MILLISECONDS)

                // Гистограмма времени обработки
                registry.timer("debezium.event.processing.duration",
                    "operation", operation,
                    "table", table
                ).record(processingTime, TimeUnit.MILLISECONDS)
            }

        } catch (ex: Exception) {
            logger.error(ex) { "Error processing change event" }
            metrics.incrementError(ex.javaClass.simpleName)

            // Счетчик ошибок по типу
            registry.counter("debezium.processing.errors",
                "exception", ex.javaClass.simpleName
            ).increment()
        }
    }

    private fun handleCreate(eventData: JsonNode, table: String) {
        val after = eventData.get("after")
        println("----- got after [$after]")
        logger.info { "handling create event, new data [${after.get("data").asText()}] stored}" }
    }

    private fun handleUpdate(eventData: JsonNode, table: String) {
        val before = eventData.get("before")
        val after = eventData.get("after")
        logger.info { "UPDATE in $table:" }
        logger.info { "Before: ${before.toPrettyString()}" }
        logger.info { "After: ${after.toPrettyString()}" }
    }

    private fun handleDelete(eventData: JsonNode, table: String) {
        val before = eventData.get("before")
        logger.info { "DELETE from $table: ${before.toPrettyString()}" }

    }

    private fun handleRead(eventData: JsonNode, table: String) {
        val after = eventData.get("after")
        logger.info { "READ from $table: ${after.toPrettyString()}" }
    }
}