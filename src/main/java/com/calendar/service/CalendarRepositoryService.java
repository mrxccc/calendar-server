package com.calendar.service;

import com.calendar.model.CalendarDo;
import com.calendar.model.CalendarEvent;
import com.calendar.repository.CalendarRepository;
import com.calendar.repository.CalendarEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CalendarRepositoryService {
    
    private final CalendarRepository calendarRepository;
    private final CalendarEventRepository eventRepository;
    
    @Cacheable(value = "calendars")
    public List<CalendarDo> getAllCalendars() {
        return calendarRepository.findAll();
    }
    
    @Transactional
    public CalendarEvent createEvent(CalendarEvent event) {
        return eventRepository.save(event);
    }
    
    public List<CalendarEvent> getEvents(Long calendarId, LocalDateTime start, LocalDateTime end) {
        return eventRepository.findByCalendarIdAndStartTimeBetween(calendarId, start, end);
    }
    
    @Transactional
    public void deleteEvent(Long eventId) {
        eventRepository.deleteById(eventId);
    }
} 