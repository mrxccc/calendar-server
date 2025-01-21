package com.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import java.util.HashSet;
import java.util.Set;

/**
 * 日历服务应用启动类
 */
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, DataSourceAutoConfiguration.class})
@ComponentScan(basePackages = { "org.unitedinternet.cosmo", "com.calendar.interceptors", "com.calendar" })
@EntityScan(basePackages = {"org.unitedinternet.cosmo.model.hibernate", "com.calendar.model"})
public class CalendarApplication {
    public static void main(String[] args) {
        SpringApplication.run(CalendarApplication.class, args);
    }

    @Bean
    @Primary
    public HttpFirewall firewall() {

        Set<String> httpMethods = new HashSet<>();
        httpMethods.add(HttpMethod.DELETE.name());
        httpMethods.add(HttpMethod.GET.name());
        httpMethods.add(HttpMethod.HEAD.name());
        httpMethods.add(HttpMethod.OPTIONS.name());
        httpMethods.add(HttpMethod.PATCH.name());
        httpMethods.add(HttpMethod.POST.name());
        httpMethods.add(HttpMethod.PUT.name());

        // Caldav methods go here
        httpMethods.add("PROPFIND");
        httpMethods.add("PROPPATCH");
        httpMethods.add("COPY");
        httpMethods.add("MOVE");
        httpMethods.add("REPORT");
        httpMethods.add("MKTICKET");
        httpMethods.add("DELTICKET");
        httpMethods.add("ACL");
        httpMethods.add("MKCOL");
        httpMethods.add("MKCALENDAR");

        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowedHttpMethods(httpMethods);
        return firewall;
    }
} 