package com.skillsync.session.controller;

import com.skillsync.session.dto.SessionResponse;
import com.skillsync.session.service.command.SessionCommandService;
import com.skillsync.session.service.query.SessionQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private SessionCommandService sessionCommandService;
    @MockitoBean private SessionQueryService sessionQueryService;

    @Test
    @DisplayName("GET /api/sessions/{id} - returns session")
    void getSession_shouldReturn200() throws Exception {
        SessionResponse response = new SessionResponse(1L, 2L, 3L, "Java", "Desc",
                LocalDateTime.now().plusDays(2), 60, null, "REQUESTED", null, LocalDateTime.now());
        when(sessionQueryService.getSessionById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/sessions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topic").value("Java"));
    }

    @Test
    @DisplayName("GET /api/sessions/{id} - not found returns 400")
    void getSession_shouldReturn400WhenNotFound() throws Exception {
        when(sessionQueryService.getSessionById(999L)).thenThrow(new RuntimeException("Session not found: 999"));

        mockMvc.perform(get("/api/sessions/999"))
                .andExpect(status().isBadRequest());
    }
}
