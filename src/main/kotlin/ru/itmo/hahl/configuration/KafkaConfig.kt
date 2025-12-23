package ru.itmo.hahl.configuration

import jakarta.annotation.PostConstruct
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.config.KafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties

@Configuration
class KafkaConfig {

    private val logger = LoggerFactory.getLogger(KafkaConfig::class.java)

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Value("\${spring.kafka.consumer.group-id}")
    private lateinit var consumerGroupId: String

    @Value("\${app.kafka.topic}")
    private lateinit var topic: String

    @PostConstruct
    fun init() {
        logger.info("KafkaConfig inited")
        logger.info("Bootstrap servers: $bootstrapServers")
        logger.info("Consumer group id: $consumerGroupId")
        logger.info("Topic: $topic")
    }

    // 1. Конфигурация консьюмера
    @Bean
    fun consumerConfigs(): Map<String, Any> {
        val props = HashMap<String, Any>()
        props[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ConsumerConfig.GROUP_ID_CONFIG] = consumerGroupId
        props[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        props[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        props[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringDeserializer"
        props[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        props[ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG] = "10"
        props[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = "1"

        // ОЧЕНЬ ВАЖНО: отключаем DNS lookup
        props[CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG] = "use_all_dns_ips"
        // Принудительно используем IP адрес
        props[CommonClientConfigs.CLIENT_ID_CONFIG] = "host-consumer-${System.currentTimeMillis()}"

        return props
    }

    // 2. Фабрика консьюмеров
    @Bean
    @Primary
    fun consumerFactory(): ConsumerFactory<String, String> {
        logger.info("Creating ConsumerFactory with bootstrap: $bootstrapServers and consumer group id $consumerGroupId")
        return DefaultKafkaConsumerFactory(consumerConfigs())
    }

    // 3. Фабрика контейнеров для листенеров
    @Bean
    @Primary
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, String>
    ): KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, String>> {
        logger.info("Creating KafkaListenerContainerFactory")

        val factory = ConcurrentKafkaListenerContainerFactory<String, String>()
        factory.consumerFactory = consumerFactory
        factory.setConcurrency(1)

        // Настраиваем контейнер
        val containerProperties = factory.containerProperties
        containerProperties.pollTimeout = 3000
        containerProperties.ackMode = ContainerProperties.AckMode.BATCH

        return factory
    }

    // 4. Конфигурация продюсера
    @Bean
    fun producerConfigs(): Map<String, Any> {
        val props = HashMap<String, Any>()
        props[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = "org.apache.kafka.common.serialization.StringSerializer"
        props[CommonClientConfigs.CLIENT_DNS_LOOKUP_CONFIG] = "use_all_dns_ips"
        return props
    }

    // 5. Фабрика продюсеров
    @Bean
    fun producerFactory(): ProducerFactory<String, String> {
        return DefaultKafkaProducerFactory(producerConfigs())
    }

    // 6. KafkaTemplate для отправки сообщений
    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, String> {
        return KafkaTemplate(producerFactory())
    }
}