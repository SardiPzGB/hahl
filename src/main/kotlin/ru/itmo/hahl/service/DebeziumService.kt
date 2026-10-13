package ru.itmo.hahl.service

import io.debezium.engine.DebeziumEngine
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import mu.KLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import ru.itmo.hahl.metrics.DebeziumMetrics
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

@Service
@ConditionalOnProperty(value = ["app.debezium.enabled"], havingValue = "true")
class DebeziumService(
    private val debeziumEngine: DebeziumEngine<io.debezium.engine.ChangeEvent<String, String>>,
    private val debeziumExecutor: ExecutorService,
    private val metrics: DebeziumMetrics,
    private val registry: MeterRegistry
) {

    companion object : KLogging()

    private val isRunning = AtomicBoolean(false)
    private val startTime = AtomicLong(0L)
    private val uptimeGauge = AtomicLong(0L)

    @PostConstruct
    fun start() {
        logger.info { "Starting Debezium engine..." }
        startTime.set(System.currentTimeMillis())

        // Инициализация Gauge для времени работы
        Gauge.builder("debezium.uptime.seconds") {
            if (isRunning.get()) (System.currentTimeMillis() - startTime.get()) / 1000.0 else 0.0
        }
            .description("Debezium engine uptime in seconds")
            .register(registry)

        Gauge.builder("debezium.uptime.gauge") { uptimeGauge.get().toDouble() }
            .description("Debezium engine uptime gauge")
            .register(registry)

        try {
            debeziumExecutor.execute {
                isRunning.set(true)
                metrics.setActiveConnectors(1)

                try {
                    debeziumEngine.run()
                } catch (ex: Exception) {
                    logger.error(ex) { "Debezium engine stopped with error" }
                    isRunning.set(false)
                    metrics.setActiveConnectors(0)
                    metrics.incrementEngineRestarts()
                }
            }

            // Проверка запуска
            Thread.sleep(1000)
            if (isRunning.get()) {
                logger.info { "Debezium engine started successfully" }
                metrics.incrementEngineStarts()
            }

        } catch (e: Exception) {
            logger.error(e) { "Failed to start Debezium engine" }
            metrics.setActiveConnectors(0)
            metrics.incrementEngineStartErrors()
        }
    }

    @PreDestroy
    fun stop() {
        logger.info { "Stopping Debezium engine..." }

        try {
            debeziumEngine.close()
            isRunning.set(false)
            metrics.setActiveConnectors(0)

            debeziumExecutor.shutdown()
            if (!debeziumExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                debeziumExecutor.shutdownNow()
            }

            metrics.incrementEngineStops()
            logger.info { "Debezium engine stopped" }

        } catch (e: Exception) {
            logger.error(e) { "Error stopping Debezium engine" }
            metrics.incrementEngineStopErrors()
        }
    }

    fun isEngineRunning(): Boolean = isRunning.get()

    fun updateUptime() {
        if (isRunning.get()) {
            uptimeGauge.set((System.currentTimeMillis() - startTime.get()) / 1000)
        }
    }
}