package com.calendar.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 日历事件实体类
 */
@Entity
@Table(name = "calendar_events")
@Data
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "calendar_id", nullable = false)
    private CalendarDo calendar;

    @Column(nullable = false)
    private String summary;

    private String description;

    private String title;

    private String location;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(name = "external_id")
    private String externalId;

    @Version
    private Long version;

    // getters and setters
} 