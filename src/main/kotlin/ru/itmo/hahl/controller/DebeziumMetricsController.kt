package ru.itmo.hahl.controller

import io.micrometer.core.instrument.MeterRegistry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ru.itmo.hahl.metrics.DebeziumMetrics
import ru.itmo.hahl.service.DebeziumService
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@RestController
@RequestMapping("/api/debezium")
class DebeziumMetricsController(
    private val registry: MeterRegistry,
    private val metrics: DebeziumMetrics,
    private val debeziumService: DebeziumService
) {

    @GetMapping("/metrics/summary")
    fun getMetricsSummary(): Map<String, Any> {
        val totalEventsCounter = registry.find("debezium.events.total").counter()
        val errorCounter = registry.find("debezium.errors.total").counter()
        val processingTimer = registry.find("debezium.processing.time").timer()

        return mapOf(
            "status" to if (debeziumService.isEngineRunning()) "RUNNING" else "STOPPED",
            "totalEvents" to (totalEventsCounter?.count() ?: 0.0),
            "totalErrors" to (errorCounter?.count() ?: 0.0),
            "averageProcessingTimeMs" to (processingTimer?.mean(TimeUnit.MILLISECONDS) ?: 0.0),
            "maxProcessingTimeMs" to (processingTimer?.max(TimeUnit.MILLISECONDS) ?: 0.0),
            "activeConnectors" to (registry.find("debezium.connectors.active").gauge()?.value() ?: 0.0),
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            "uptimeSeconds" to (registry.find("debezium.uptime.seconds").gauge()?.value() ?: 0.0)
        )
    }

    @GetMapping("/metrics/events-by-table")
    fun getEventsByTable(): Map<String, Any> {
        val eventsByTable = mutableMapOf<String, Double>()

        registry.find("debezium.events.by.type").counters().forEach { counter ->
            val table = counter.id.tags.firstOrNull { it.key == "table" }?.value ?: "unknown"
            val operation = counter.id.tags.firstOrNull { it.key == "operation" }?.value ?: "unknown"
            eventsByTable["${table}_${operation}"] = counter.count()
        }

        return mapOf(
            "eventsByTable" to eventsByTable,
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @GetMapping("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "engineRunning" to debeziumService.isEngineRunning(),
            "status" to if (debeziumService.isEngineRunning()) "HEALTHY" else "UNHEALTHY",
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }

    @GetMapping("/status")
    fun status(): Map<String, Any> {
        val errorsByType = mutableMapOf<String, Double>()
        registry.find("debezium.errors.by.type").counters().forEach { counter ->
            val exception = counter.id.tags.firstOrNull { it.key == "exception" }?.value ?: "unknown"
            errorsByType[exception] = counter.count()
        }

        return mapOf(
            "engineStatus" to if (debeziumService.isEngineRunning()) "ACTIVE" else "INACTIVE",
            "totalEventsProcessed" to (registry.find("debezium.events.total").counter()?.count() ?: 0.0),
            "totalErrors" to (registry.find("debezium.errors.total").counter()?.count() ?: 0.0),
            "errorsByType" to errorsByType,
            "replicationLagMs" to (registry.find("debezium.replication.lag.millis").gauges().firstOrNull()?.value() ?: 0.0),
            "timestamp" to LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }
}