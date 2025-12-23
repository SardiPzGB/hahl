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
                val eventData = objectMapper.readTree(value)
                val operation = eventData.get("op").asText()
                val source = eventData.get("source")
                val table = source.get("table").asText()
                val tsMs = source.get("ts_ms").asLong()

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
                        handleCreate(eventData, table)
                    }
                    "u" -> {
                        metrics.incrementUpdate(table)
                        handleUpdate(eventData, table)
                    }
                    "d" -> {
                        metrics.incrementDelete(table)
                        handleDelete(eventData, table)
                    }
                    "r" -> {
                        metrics.incrementRead(table)
                        handleRead(eventData, table)
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
        logger.info { "INSERT into $table: ${after.toPrettyString()}" }

        // Бизнес-логика для создания
        when (table) {
            "users" -> {
                val userId = after.get("id")?.asLong()
                val email = after.get("email")?.asText()
                // Отправить уведомление, обновить кэш и т.д.
                logger.info { "New user created: ID=$userId, Email=$email" }
            }
            "orders" -> {
                val orderId = after.get("id")?.asLong()
                val amount = after.get("amount")?.asDouble()
                logger.info { "New order created: ID=$orderId, Amount=$amount" }
            }
        }
    }

    private fun handleUpdate(eventData: JsonNode, table: String) {
        val before = eventData.get("before")
        val after = eventData.get("after")
        logger.info { "UPDATE in $table:" }
        logger.info { "Before: ${before.toPrettyString()}" }
        logger.info { "After: ${after.toPrettyString()}" }

        // Бизнес-логика для обновления
        when (table) {
            "users" -> {
                val userId = after.get("id")?.asLong()
                logger.info { "User updated: ID=$userId" }
            }
        }
    }

    private fun handleDelete(eventData: JsonNode, table: String) {
        val before = eventData.get("before")
        logger.info { "DELETE from $table: ${before.toPrettyString()}" }

        // Бизнес-логика для удаления
        when (table) {
            "users" -> {
                val userId = before.get("id")?.asLong()
                logger.info { "User deleted: ID=$userId" }
            }
        }
    }

    private fun handleRead(eventData: JsonNode, table: String) {
        val after = eventData.get("after")
        logger.debug { "READ from $table: ${after.toPrettyString()}" }
    }
}