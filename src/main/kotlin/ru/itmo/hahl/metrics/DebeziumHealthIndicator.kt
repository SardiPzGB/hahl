package ru.itmo.hahl.metrics

import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import ru.itmo.hahl.service.DebeziumService

@Component
@ConditionalOnProperty(value = ["app.debezium.enabled"], havingValue = "true")
class DebeziumHealthIndicator(
    private val debeziumService: DebeziumService
) : HealthIndicator {

    override fun health(): Health {
        return if (debeziumService.isEngineRunning()) {
            Health.up()
                .withDetail("status", "Debezium engine is running")
                .withDetail("connectors", 1)
                .withDetail("component", "CDC Engine")
                .build()
        } else {
            Health.down()
                .withDetail("status", "Debezium engine is not running")
                .withDetail("component", "CDC Engine")
                .build()
        }
    }
}