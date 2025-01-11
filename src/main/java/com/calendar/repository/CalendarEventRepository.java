package com.calendar.repository;

import com.calendar.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {
    
    List<CalendarEvent> findByCalendarIdAndStartTimeBetween(
        Long calendarId, 
        LocalDateTime start, 
        LocalDateTime end
    );
    
    Optional<CalendarEvent> findByExternalId(String externalId);
} 