package com.calendar.controller;

import com.calendar.service.GoogleOAuth2Service;
import com.calendar.service.GoogleCalendarService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class GoogleCalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetAuthorizationUrl() throws Exception {
        mockMvc.perform(get("/api/calendars/google/auth")
                .param("userId", "test@example.com"))
                .andExpect(status().isOk());
    }
} 