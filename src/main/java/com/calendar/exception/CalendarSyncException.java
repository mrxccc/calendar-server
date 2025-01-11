package com.calendar.exception;

/**
 * 日历同步异常类
 */
public class CalendarSyncException extends RuntimeException {
    
    public CalendarSyncException(String message) {
        super(message);
    }

    public CalendarSyncException(String message, Throwable cause) {
        super(message, cause);
    }
} 