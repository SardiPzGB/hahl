package ru.itmo.hahl.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

@Service
@ConditionalOnProperty(value = ["app.kafka.enabled"], havingValue = "true")
class KafkaMessageProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    private val logger = LoggerFactory.getLogger(KafkaMessageProducer::class.java)

    @Value("\${app.kafka.topic}")
    private lateinit var topic: String

    @EventListener(ApplicationReadyEvent::class)
    fun refreshMetadata() {
        logger.info("Updating metadata by force, topic '$topic'")

        // Принудительно запрашиваем метаданные топика
        kafkaTemplate.execute { operations ->
            operations.flush()
            true
        }

        // Альтернативный способ
        try {
            val producer = kafkaTemplate.producerFactory.createProducer()
            producer.partitionsFor(topic) // Этот вызов обновит метаданные
            producer.close()
            logger.info("Metadata refreshed")
        } catch (e: Exception) {
            logger.error("Error while refreshing metadata", e)
        }
    }

    /**
     * Простая отправка сообщения (fire-and-forget)
     */
    fun sendMessage(message: String) {
        logger.info("📤 Отправляю сообщение в топик '$topic': $message")
        kafkaTemplate.send(topic, message)
    }

    /**
     * Отправка сообщения с ключом
     */
    fun sendMessage(key: String, message: String) {
        logger.info("📤 Отправляю сообщение с ключом '$key': $message")
        kafkaTemplate.send(topic, key, message)
    }

    /**
     * Отправка с callback для обработки результата
     */
    fun sendMessageWithCallback(message: String) {
        logger.info("📤 Отправляю сообщение с callback: $message")

        val future: CompletableFuture<SendResult<String, String>> =
            kafkaTemplate.send(topic, message)

        future.whenComplete { result, exception ->
            if (exception == null) {
                logger.info("✅ Сообщение успешно отправлено")
                logger.info("   Партиция: ${result?.recordMetadata?.partition()}")
                logger.info("   Оффсет: ${result?.recordMetadata?.offset()}")
            } else {
                logger.error("❌ Ошибка отправки сообщения", exception)
            }
        }
    }

    /**
     * Отправка сообщения синхронно (ожидание подтверждения)
     */
    fun sendMessageSync(message: String): Boolean {
        return try {
            logger.info("📤 Синхронная отправка: $message")
            val result = kafkaTemplate.send(topic, message).get()
            logger.info("✅ Отправлено успешно. Партиция: ${result.recordMetadata.partition()}")
            true
        } catch (e: Exception) {
            logger.error("❌ Ошибка синхронной отправки", e)
            false
        }
    }
}