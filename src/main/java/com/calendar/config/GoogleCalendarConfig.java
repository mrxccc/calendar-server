package com.calendar.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import jakarta.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.security.GeneralSecurityException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpRequest;

/**
 * Google Calendar配置类
 */
@Configuration
public class GoogleCalendarConfig {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);

    @Value("${http.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${http.proxy.port:#{null}}")
    private String proxyPort;

    @Value("${google.oauth.credentials.path}")
    private Resource credentialsPath;

    @Value("${google.oauth.tokens.directory.path}")
    private String tokensDirectoryPath;

    @PostConstruct
    public void init() {
        // 如果配置了代理，设置系统属性
        if (proxyHost != null && proxyPort != null) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyHost", proxyHost);
            System.setProperty("https.proxyPort", proxyPort);
        }
    }

    @Bean
    public NetHttpTransport netHttpTransport() throws GeneralSecurityException, IOException {
        NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        transport.createRequestFactory(request -> {
            request.setConnectTimeout(60000);  // 60秒连接超时
            request.setReadTimeout(60000);     // 60秒读取超时
        });
        return transport;
    }

    @Bean
    public GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow(NetHttpTransport transport) throws IOException {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY,
                new InputStreamReader(credentialsPath.getInputStream()));

        return new GoogleAuthorizationCodeFlow.Builder(
                transport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
                .setAccessType("offline")
                .build();
    }
} 