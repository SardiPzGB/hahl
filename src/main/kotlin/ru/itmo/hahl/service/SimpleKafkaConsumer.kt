package ru.itmo.hahl.service


import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class SimpleKafkaConsumer {
    private val logger = LoggerFactory.getLogger(SimpleKafkaConsumer::class.java)

    @Value("\${app.kafka.topic}")
    private lateinit var topic: String

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var consumerGroupId: String

    @PostConstruct
    fun init() {
        logger.info("Simple Kafka Consumer inited")
        logger.info("Bootstrap servers: $bootstrapServers")
        logger.info("Consumer group id: $consumerGroupId")
        logger.info("Topic: $topic")
    }

    // Способ 1: Использование @KafkaListener с явным containerFactory
    @KafkaListener(
        topics = ["\${app.kafka.topic}"],
        groupId = "\${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun listen(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_KEY) key: String?,
        @Header(KafkaHeaders.RECEIVED_PARTITION) partition: Int,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String
    ) {
        logger.info(
            """ Message received:
               Topic: $topic
               Key: ${key ?: "no"}
               Partition: $partition
               Message: $message
        """.trimIndent()
        )

        processMessage(message)
    }

    private fun processMessage(message: String) {
        // Ваша бизнес-логика
        logger.info("Working with: $message")
    }
}