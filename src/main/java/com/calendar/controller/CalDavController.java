package com.calendar.controller;

import com.calendar.model.CalendarCollection;
import com.calendar.service.CalDavService;
import com.github.caldav4j.CalDAVCollection;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.DateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

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

    @PostMapping("/create")
    @Operation(summary = "创建CalDAV日历")
    public ResponseEntity<String> createCalendar(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String calendarName) {
        try {
            calDavService.createCalendar(username, password, calendarName);
            return ResponseEntity.ok("CalDAV日历创建成功");
        } catch (Exception e) {
            log.error("创建CalDAV日历失败", e);
            return ResponseEntity.internalServerError().body("创建失败: " + e.getMessage());
        }
    }

    @PostMapping("/createEvent")
    @Operation(summary = "创建CalDAV日历事件")
    public ResponseEntity<String> createCalendarEvent(
            @RequestParam String calendarUrl,
            @RequestParam String summary,
            @RequestParam String location,
            @RequestParam String start,
            @RequestParam String end) {
        try {
            DateTime startDateTime = new DateTime(start);
            DateTime endDateTime = new DateTime(end);
            calDavService.createCalendarEvent(calendarUrl, summary, location, startDateTime, endDateTime);
            return ResponseEntity.ok("日历事件创建成功");
        } catch (Exception e) {
            log.error("创建CalDAV日历事件失败", e);
            return ResponseEntity.internalServerError().body("创建失败: " + e.getMessage());
        }
    }

    @PutMapping("/updateEvent")
    @Operation(summary = "更新CalDAV日历事件")
    public ResponseEntity<String> updateCalendarEvent(
            @RequestParam String calendarUrl,
            @RequestParam String eventUid,
            @RequestParam String newSummary,
            @RequestParam String newLocation,
            @RequestParam String newStart,
            @RequestParam String newEnd) {
        try {
            DateTime newStartDateTime = new DateTime(newStart);
            DateTime newEndDateTime = new DateTime(newEnd);
            calDavService.updateCalendarEvent(calendarUrl, eventUid, newSummary, newLocation, newStartDateTime, newEndDateTime);
            return ResponseEntity.ok("日历事件更新成功");
        } catch (IOException | ParseException e) {
            log.error("更新CalDAV日历事件失败", e);
            return ResponseEntity.internalServerError().body("更新失败: " + e.getMessage());
        }
    }

    /**
     * 获取日历集合列表
     * @return
     */
    @GetMapping("/calendars")
    public List<CalendarCollection> getCalendars() {
        return calDavService.getCalendarCollections();
    }

    /**
     * 获取单个日历集合
     * @param uid
     * @return
     */
    // 获取单个日历集合详情
    @GetMapping("/calendars/{uid}")
    public CalendarCollection getCalendar(@PathVariable String uid) {
        return calDavService.getCalendarCollectionDetails(uid);
    }
} 