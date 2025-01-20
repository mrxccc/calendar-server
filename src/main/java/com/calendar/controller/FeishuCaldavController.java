package com.calendar.controller;

import com.calendar.service.FeishuCalendarService;
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
@RequestMapping("/api/calendars/feishu")
@Tag(name = "飞书caldav", description = "飞书caldav")
@RequiredArgsConstructor
@Slf4j
public class FeishuCaldavController {

    private final FeishuCalendarService feishuCalendarService;

    @GetMapping("/propfind")
    @Operation(summary = "飞书caldav propfind请求")
    public ResponseEntity<String> propFindDisplayName(@RequestParam String userId) throws IOException {
        String authUrl = feishuCalendarService.propFindDisplayName();
        return ResponseEntity.ok(authUrl);
    }
}