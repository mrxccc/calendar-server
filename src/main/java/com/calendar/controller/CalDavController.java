package com.calendar.controller;

import com.calendar.service.CalDavService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/calendars/caldav")
@Tag(name = "CalDAV日历管理", description = "CalDAV日历相关的API接口")
@RequiredArgsConstructor
@Slf4j
public class CalDavController {

    private final CalDavService calDavService;

    @PostMapping("/sync")
    @Operation(summary = "同步CalDAV日历")
    public ResponseEntity<String> syncCalendar(
            @RequestParam String calendarUrl,
            @RequestParam String username,
            @RequestParam String password) {
        try {
            calDavService.syncCalendar(calendarUrl, username, password);
            return ResponseEntity.ok("CalDAV日历同步成功");
        } catch (Exception e) {
            log.error("同步CalDAV日历失败", e);
            return ResponseEntity.internalServerError().body("同步失败: " + e.getMessage());
        }
    }
} 