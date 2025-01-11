package com.calendar.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CalendarSyncException.class)
    public ResponseEntity<String> handleCalendarSyncException(CalendarSyncException e) {
        log.error("日历同步失败", e);
        return ResponseEntity.internalServerError().body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.internalServerError().body("系统内部错误");
    }
} 