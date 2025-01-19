package com.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * 日历服务应用启动类
 */
@SpringBootApplication
@ServletComponentScan(basePackages = {"com.calendar.hellocaldav"})
@EnableJpaRepositories(basePackages = "com.calendar.repository")
@EntityScan(basePackages = "com.calendar.model")
public class CalendarApplication {
    public static void main(String[] args) {
        SpringApplication.run(CalendarApplication.class, args);
    }
} 