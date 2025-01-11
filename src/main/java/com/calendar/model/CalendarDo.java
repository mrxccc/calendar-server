package com.calendar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 日历实体类
 */
@Entity
@Table(name = "calendars")
@Data
public class CalendarDo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String owner;

    @Enumerated(EnumType.STRING)
    private CalendarType type; // LOCAL, GOOGLE, ICAL等

    @Column(name = "external_id")
    private String externalId; // 外部日历的ID（如Google日历ID）

    @Column(name = "sync_token")
    private String syncToken; // 用于增量同步

    @Column(name = "last_sync")
    private LocalDateTime lastSync;

    // getters and setters
} 