package com.calendar.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.net.Proxy;
import java.net.InetSocketAddress;

/**
 * 代理配置类
 */
@Configuration
@Slf4j
@Getter
public class ProxyConfig {

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private String proxyPort;

    private Proxy proxy;

    @PostConstruct
    public void init() {
        if (proxyHost != null && proxyPort != null) {
            try {
                proxy = new Proxy(
                    Proxy.Type.HTTP,
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort))
                );
                log.info("代理已配置: {}:{}", proxyHost, proxyPort);
            } catch (Exception e) {
                log.error("代理配置失败: {}", e.getMessage());
            }
        } else {
            log.info("未配置代理");
        }
    }

    /**
     * 获取配置的代理
     */
    public Proxy getConfiguredProxy() {
        return proxy;
    }

    /**
     * 检查是否配置了代理
     */
    public boolean isProxyConfigured() {
        return proxy != null;
    }
} 