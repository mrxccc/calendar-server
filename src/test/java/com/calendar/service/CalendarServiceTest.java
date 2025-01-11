package com.calendar.service;

import com.calendar.model.CalendarDo;
import com.calendar.repository.CalendarRepository;
import com.calendar.repository.CalendarEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CalendarServiceTest {

    @Autowired
    private CalendarService calendarService;

    @MockBean
    private CalendarRepository calendarRepository;

    @MockBean
    private CalendarEventRepository eventRepository;

    @Test
    void getAllCalendars_ShouldReturnCalendarList() {
        // Given
        CalendarDo calendar = new CalendarDo();
        calendar.setName("Test Calendar");
        when(calendarRepository.findAll()).thenReturn(Arrays.asList(calendar));

        // When
        List<CalendarDo> result = calendarService.getAllCalendars();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Calendar", result.get(0).getName());
    }
} 