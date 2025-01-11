package com.calendar.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger配置类
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI calendarOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("日历服务 API")
                .description("日历服务系统的REST API文档")
                .version("v1.0.0"));
    }
} 