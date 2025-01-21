package com.calendar.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Data source configuration that starts an embedded Maria DB instance before data source is created.
 * 
 * @author daniel grigore
 *
 */
@Configuration
@ConfigurationProperties("spring.datasource")
public class DataSourceConfig extends HikariConfig {


    @Bean
    public DataSource ds() {
        return new HikariDataSource(this);
    }
}
