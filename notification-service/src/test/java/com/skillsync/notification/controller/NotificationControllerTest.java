package com.skillsync.notification.controller;

import com.skillsync.notification.service.NotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private NotificationService notificationService;

    @Test
    @DisplayName("GET /api/notifications/unread/count - returns count")
    void getUnreadCount_shouldReturn200() throws Exception {
        when(notificationService.getUnreadCount(100L)).thenReturn(5L);

        mockMvc.perform(get("/api/notifications/unread/count").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read - marks as read")
    void markAsRead_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/notifications/1/read"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT /api/notifications/read-all - marks all as read")
    void markAllAsRead_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/notifications/read-all").header("X-User-Id", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /api/notifications/{id} - deletes notification")
    void deleteNotification_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/notifications/1"))
                .andExpect(status().isOk());
    }
}
