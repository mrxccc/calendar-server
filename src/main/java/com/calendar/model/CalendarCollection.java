package com.calendar.model;

import lombok.Data;

import java.util.List;

@Data
public class CalendarCollection {

    private String displayName; // 日历名称
    private String href; // 日历的链接
    List<CalendarEvent> events;
}
