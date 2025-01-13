package com.calendar.controller;

import java.io.IOException;
import java.util.Calendar;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.calendar.service.BedeworkCalendarService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.DateTime;

@RestController
@RequestMapping("/api/calendars/bedework")
@Tag(name = "Bedework日历管理", description = "Bedework日历相关的API接口")
@RequiredArgsConstructor
@Slf4j
public class BedeworkCalendarController {

    private final BedeworkCalendarService bedeworkCalendarService;

    @GetMapping("/getCalendar")
    @Operation(summary = "获取Bedework日历")
    public ResponseEntity<String> getCalendar() {
        try {
            String calendarData = bedeworkCalendarService.getCalendar();
            return ResponseEntity.ok(calendarData);
        } catch (IOException e) {
            log.error("获取Bedework日历失败", e);
            return ResponseEntity.internalServerError().body("获取失败: " + e.getMessage());
        }
    }

    @PostMapping("/createEvent")
    @Operation(summary = "创建Bedework日历事件")
    public ResponseEntity<String> createCalendarEvent(
            @RequestParam String calendarPath,
            @RequestParam String summary,
            @RequestParam String location,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        try {
            DateTime startDateTime;
            DateTime endDateTime;

            if (start == null || end == null) {
                // 设置默认时间为明天到后天
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, 1);
                startDateTime = new DateTime(calendar.getTime());

                calendar.add(Calendar.DAY_OF_MONTH, 1);
                endDateTime = new DateTime(calendar.getTime());
            } else {
                startDateTime = new DateTime(start);
                endDateTime = new DateTime(end);
            }

            bedeworkCalendarService.createCalendarEvent(calendarPath, summary, location, startDateTime, endDateTime);
            return ResponseEntity.ok("日历事件创建成功");
        } catch (Exception e) {
            log.error("创建Bedework日历事件失败", e);
            return ResponseEntity.internalServerError().body("创建失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/deleteEvent")
    @Operation(summary = "删除Bedework日历事件")
    public ResponseEntity<String> deleteCalendarEvent(
            @RequestParam String calendarPath,
            @RequestParam String eventUid) {
        try {
            bedeworkCalendarService.deleteCalendarEvent(calendarPath, eventUid);
            return ResponseEntity.ok("日历事件删除成功");
        } catch (IOException e) {
            log.error("删除Bedework日历事件失败", e);
            return ResponseEntity.internalServerError().body("删除失败: " + e.getMessage());
        }
    }
} 