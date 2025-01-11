package com.calendar.controller;

import com.calendar.model.CalendarDo;
import com.calendar.service.CalendarService;
import com.calendar.service.GoogleCalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * 日历控制器
 */
@RestController
@RequestMapping("/api/calendars")
@Tag(name = "日历管理", description = "日历相关的API接口")
public class CalendarController {
    @Autowired
    private CalendarService calendarService;

    @Autowired
    private GoogleCalendarService googleCalendarService;

    @GetMapping
    @Operation(summary = "获取所有日历列表")
    public ResponseEntity<List<CalendarDo>> getAllCalendars() {
        return ResponseEntity.ok(calendarService.getAllCalendars());
    }
} 