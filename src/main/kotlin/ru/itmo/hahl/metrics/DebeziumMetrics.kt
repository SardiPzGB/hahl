package ru.itmo.hahl.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import ru.itmo.hahl.service.ChangeEventHandler.Companion.logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Component
class DebeziumMetrics(
    private val registry: MeterRegistry
) {

    // Счетчики для операций
    private val totalEventsCounter: Counter = Counter.builder("debezium.events.total")
        .description("Total number of processed events")
        .register(registry)

    private val insertCounters = ConcurrentHashMap<String, Counter>()
    private val updateCounters = ConcurrentHashMap<String, Counter>()
    private val deleteCounters = ConcurrentHashMap<String, Counter>()

    // Счетчики для ошибок
    private val errorCounter: Counter = Counter.builder("debezium.errors.total")
        .description("Total number of processing errors")
        .register(registry)

    // Таймер для обработки событий
    private val processingTimer: Timer = Timer.builder("debezium.processing.time")
        .description("Time spent processing events")
        .publishPercentiles(0.5, 0.95, 0.99)
        .register(registry)

    // Gauge для активных коннекторов
    private val activeConnectors = AtomicInteger(0)

    // Gauge для лага репликации
    private val replicationLag = ConcurrentHashMap<String, AtomicLong>()

    // Счетчики для состояния движка
    private val engineStartsCounter: Counter = Counter.builder("debezium.engine.starts.total")
        .description("Total number of engine starts")
        .register(registry)

    private val engineStopsCounter: Counter = Counter.builder("debezium.engine.stops.total")
        .description("Total number of engine stops")
        .register(registry)

    @PostConstruct
    fun init() {
        // Инициализация Gauge метрик
        Gauge.builder("debezium.connectors.active", activeConnectors) { it.toDouble() }
            .description("Number of active Debezium connectors")
            .register(registry)

        Gauge.builder("debezium.queue.size") { getQueueSize().toDouble() }
            .description("Size of event processing queue")
            .register(registry)

        // Создание базовых счетчиков операций
        listOf("insert", "update", "delete", "read").forEach { operation ->
            Counter.builder("debezium.events.operation")
                .tag("operation", operation)
                .description("Number of events by operation")
                .register(registry)
        }
    }

    fun incrementInsert(table: String) {
        logger.info { "incrementing insert metric for table $table" }
        totalEventsCounter.increment()
        getInsertCounter(table).increment()
        registry.counter("debezium.events.operation", "operation", "insert").increment()
    }

    fun incrementUpdate(table: String) {
        totalEventsCounter.increment()
        getUpdateCounter(table).increment()
        registry.counter("debezium.events.operation", "operation", "update").increment()
    }

    fun incrementDelete(table: String) {
        totalEventsCounter.increment()
        getDeleteCounter(table).increment()
        registry.counter("debezium.events.operation", "operation", "delete").increment()
    }

    fun incrementRead(table: String) {
        totalEventsCounter.increment()
        registry.counter("debezium.events.operation", "operation", "read").increment()
    }

    fun incrementError(exceptionType: String? = null) {
        errorCounter.increment()
        if (exceptionType != null) {
            registry.counter("debezium.errors.by.type", "exception", exceptionType).increment()
        }
    }

    fun recordProcessingTime(time: Long, unit: TimeUnit) {
        processingTimer.record(time, unit)
    }

    fun setActiveConnectors(count: Int) {
        activeConnectors.set(count)
    }

    fun setReplicationLag(connector: String, lagMillis: Long) {
        replicationLag.computeIfAbsent(connector) { AtomicLong(0) }.set(lagMillis)

        // Создаем gauge для этого коннектора, если его еще нет
        if (!registry.find("debezium.replication.lag.millis").tags("connector", connector).gauges().isEmpty()) {
            Gauge.builder("debezium.replication.lag.millis") {
                replicationLag[connector]?.get()?.toDouble() ?: 0.0
            }
                .tag("connector", connector)
                .description("Replication lag in milliseconds")
                .register(registry)
        }
    }

    fun incrementEngineStarts() {
        engineStartsCounter.increment()
    }

    fun incrementEngineStops() {
        engineStopsCounter.increment()
    }

    fun incrementEngineStartErrors() {
        registry.counter("debezium.engine.start.errors.total").increment()
    }

    fun incrementEngineStopErrors() {
        registry.counter("debezium.engine.stop.errors.total").increment()
    }

    fun incrementEngineRestarts() {
        registry.counter("debezium.engine.restarts.total").increment()
    }

    private fun getInsertCounter(table: String): Counter {
        return insertCounters.computeIfAbsent(table) {
            Counter.builder("debezium.events.by.type")
                .description("Events by type and table")
                .tag("operation", "insert")
                .tag("table", table)
                .register(registry)
        }
    }

    private fun getUpdateCounter(table: String): Counter {
        return updateCounters.computeIfAbsent(table) {
            Counter.builder("debezium.events.by.type")
                .description("Events by type and table")
                .tag("operation", "update")
                .tag("table", table)
                .register(registry)
        }
    }

    private fun getDeleteCounter(table: String): Counter {
        return deleteCounters.computeIfAbsent(table) {
            Counter.builder("debezium.events.by.type")
                .description("Events by type and table")
                .tag("operation", "delete")
                .tag("table", table)
                .register(registry)
        }
    }

    private fun getQueueSize(): Int {
        // В реальном приложении здесь можно получить размер очереди из Debezium Engine
        return 0
    }

    fun getEventSizeHistogram() = registry.summary("debezium.event.size.bytes")
}