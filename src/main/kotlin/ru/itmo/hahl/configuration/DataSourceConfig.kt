package ru.itmo.hahl.configuration

import com.zaxxer.hikari.HikariDataSource
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.jdbc.DataSourceBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import javax.sql.DataSource

@Configuration
class DataSourceConfig {

    @Bean
    @Primary
    // ВАЖНО: Указываем префикс именно до блока hikari
    @ConfigurationProperties("spring.datasource.hikari")
    fun masterDataSource(): HikariDataSource {
        // Создаем конкретно HikariDataSource, чтобы @ConfigurationProperties
        // смог вызвать методы setJdbcUrl, setMaximumPoolSize и т.д.
        return DataSourceBuilder.create()
            .type(HikariDataSource::class.java)
            .build()
    }

    @Bean
    // Уточнение: имя бина будет masterTemplate,
    // внедряем наш masterDataSource
    fun masterTemplate(masterDataSource: DataSource): NamedParameterJdbcTemplate {
        return NamedParameterJdbcTemplate(masterDataSource)
    }

}