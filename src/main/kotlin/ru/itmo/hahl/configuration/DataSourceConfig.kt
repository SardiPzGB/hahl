//package ru.itmo.hahl.configuration
//
//import org.springframework.beans.factory.annotation.Qualifier
//import org.springframework.boot.context.properties.ConfigurationProperties
//import org.springframework.boot.jdbc.DataSourceBuilder
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.context.annotation.Primary
//import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
//import javax.sql.DataSource
//
//@Configuration
//class DataSourceConfig {
//
//    @Bean
//    @Primary
//    @ConfigurationProperties("spring.datasource.primary")
//    fun masterDataSource() = DataSourceBuilder.create().build()
//
//    @Bean
//    @Qualifier("master")
//    fun masterTemplate(
//        @Qualifier("masterDataSource")
//        dataSource: DataSource
//    ) = NamedParameterJdbcTemplate(dataSource)
//
//    @Bean
//    @Qualifier("replica")
//    @ConfigurationProperties("spring.datasource.replica")
//    fun replicaDataSource() = DataSourceBuilder.create().build()
//
//    @Bean
//    @Qualifier("replica")
//    fun replicaTemplate(
//        @Qualifier("replicaDataSource")
//        dataSource: DataSource
//    ) = NamedParameterJdbcTemplate(dataSource)
//}