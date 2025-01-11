package com.calendar.controller;

import com.calendar.service.GoogleCalendarService;
import com.calendar.service.GoogleOAuth2Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Google日历控制器
 */
@RestController
@RequestMapping("/api/calendars/google")
@Tag(name = "Google日历管理", description = "Google日历相关的API接口")
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarController {

    private final GoogleOAuth2Service oAuth2Service;
    private final GoogleCalendarService calendarService;

    @GetMapping("/auth")
    @Operation(summary = "获取Google授权URL")
    public ResponseEntity<String> getAuthorizationUrl(@RequestParam String userId) throws IOException {
        String authUrl = oAuth2Service.generateAuthorizationUrl(userId);
        return ResponseEntity.ok(authUrl);
    }

    @GetMapping("/oauth2callback")
    @Operation(summary = "处理Google OAuth2回调")
    public ResponseEntity<String> handleOAuth2Callback(
            @RequestParam String state,
            @RequestParam String code) throws IOException {
        return ResponseEntity.ok(oAuth2Service.handleCallback(state, code));
    }

    @PostMapping("/sync")
    @Operation(summary = "同步Google日历")
    public ResponseEntity<Void> syncCalendar(
            @RequestParam String userId,
            @RequestParam String calendarId) {
        calendarService.syncCalendar(userId, calendarId)
                .exceptionally(throwable -> {
                    log.error("同步日历失败", throwable);
                    return null;
                });
        return ResponseEntity.accepted().build();
    }
} 