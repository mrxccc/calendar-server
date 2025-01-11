package com.calendar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.io.IOException;

/**
 * Google API健康检查服务
 */
@Service
@Slf4j
public class GoogleApiHealthCheck {

    private static final String GOOGLE_API_TEST_URL = "https://www.googleapis.com/discovery/v1/apis";
    private static final int TIMEOUT_MS = 5000;

    @Value("${http.proxy.host}")
    private String proxyHost;

    @Value("${http.proxy.port}")
    private String proxyPort;
    /**
     * 检查Google API的连通性
     */
    public boolean checkGoogleApiAccess() {
        try {
            log.info("开始检查Google API连通性...");
            
            // 创建连接
            URL url = new URL(GOOGLE_API_TEST_URL);
            HttpURLConnection connection;
            
            if (proxyHost != null && proxyPort != null) {
                log.info("使用代理: {}:{}", proxyHost, proxyPort);
                Proxy proxy = new Proxy(Proxy.Type.HTTP, 
                    new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
                connection = (HttpURLConnection) url.openConnection(proxy);
            } else {
                log.info("不使用代理进行连接");
                connection = (HttpURLConnection) url.openConnection();
            }
            
            // 设置连接参数
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            
            // 尝试连接
            int responseCode = connection.getResponseCode();
            log.info("Google API响应代码: {}", responseCode);
            
            boolean isAccessible = (responseCode == 200);
            log.info("Google API {} 访问", isAccessible ? "可以" : "不可以");
            
            return isAccessible;
            
        } catch (IOException e) {
            log.error("检查Google API连通性时发生错误: {}", e.getMessage());
            return false;
        }
    }
} 