package com.calendar.service;

import com.calendar.config.ProxyConfig;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.apache.v2.ApacheHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.apache.http.HttpHost;
import org.apache.http.impl.client.HttpClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Google OAuth2认证服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleOAuth2Service {

    private final GoogleAuthorizationCodeFlow flow;
    private final ProxyConfig proxyConfig;
    private static final int TIMEOUT_SECONDS = 60;

    private final GoogleApiHealthCheck apiHealthCheck;

    @Value("${google.oauth.callback.uri}")
    private String callbackUri;

    @Value("${google.oauth.credentials.path}")
    private Resource credentialsPath;

    /**
     * 生成授权URL
     */
    public String generateAuthorizationUrl(String userId) throws IOException {
        // 首先检查API连通性
        if (!apiHealthCheck.checkGoogleApiAccess()) {
            log.error("Google API 无法访问，请检查网络连接或代理设置");
            throw new IOException("无法访问Google API");
        }

        log.debug("正在为用户 {} 生成授权URL", userId);
        GoogleAuthorizationCodeRequestUrl url = flow.newAuthorizationUrl()
                .setRedirectUri(callbackUri)
                .setState(userId);
        log.debug("生成的授权URL: {}", url.build());
        return url.build();
    }

    /**
     * 处理OAuth回调
     */
    public String handleCallback(String userId, String code) throws IOException {
        int maxRetries = 3;
        int currentTry = 0;
        IOException lastException = null;

        while (currentTry < maxRetries) {
            try {
                log.info("开始处理OAuth回调, 用户ID: {}, 尝试次数: {}", userId, currentTry + 1);
                
                // 配置HTTP传输和代理
                HttpTransport transport;
                if (proxyConfig.isProxyConfigured()) {
                    log.info("使用代理配置: {}:{}", proxyConfig.getProxyHost(), proxyConfig.getProxyPort());
                    HttpHost proxy = new HttpHost(
                        proxyConfig.getProxyHost(), 
                        Integer.parseInt(proxyConfig.getProxyPort())
                    );
                    
                    transport = new ApacheHttpTransport(
                        HttpClientBuilder.create()
                            .setProxy(proxy)
                            .build()
                    );
                } else {
                    transport = new NetHttpTransport();
                }
                
                // 设置超时
                transport.createRequestFactory(request -> {
                    request.setConnectTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
                    request.setReadTimeout((int) TimeUnit.SECONDS.toMillis(TIMEOUT_SECONDS));
                });

                log.debug("正在发送token请求...");
                // 使用配置的transport创建新的flow
                GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                    JacksonFactory.getDefaultInstance(),
                    new InputStreamReader(credentialsPath.getInputStream())
                );
                
                GoogleAuthorizationCodeFlow newFlow = new GoogleAuthorizationCodeFlow.Builder(
                    transport,
                    JacksonFactory.getDefaultInstance(),
                    clientSecrets,
                    flow.getScopes())
                    .setAccessType("offline")
                    .build();
                
                GoogleTokenResponse response = newFlow.newTokenRequest(code)
                    .setRedirectUri(callbackUri)
                    .execute();

                log.debug("Token请求成功，正在存储凭证...");
                flow.createAndStoreCredential(response, userId);
                String res = String.format("用户 %s 的Google Calendar授权成功", userId);
                log.info(res);
                return res;
            } catch (IOException e) {
                lastException = e;
                currentTry++;
                log.warn("OAuth回调处理失败，尝试次数：{}/{}，错误类型：{}，错误消息：{}", 
                        currentTry, maxRetries, e.getClass().getSimpleName(), e.getMessage());
                
                if (currentTry < maxRetries) {
                    try {
                        int waitTime = 1000 * currentTry;
                        log.debug("等待 {} 毫秒后重试...", waitTime);
                        Thread.sleep(waitTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
        }
        
        log.error("OAuth回调处理最终失败，详细错误：", lastException);
        throw new IOException("OAuth回调处理失败，已重试" + maxRetries + "次", lastException);
    }

    /**
     * 获取用户凭证
     */
    public Credential getCredential(String userId) throws IOException {
        return flow.loadCredential(userId);
    }
} 