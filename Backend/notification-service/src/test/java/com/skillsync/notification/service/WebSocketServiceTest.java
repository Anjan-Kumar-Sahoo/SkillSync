package com.skillsync.notification.service;

import com.skillsync.notification.dto.NotificationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @InjectMocks private WebSocketService service;

    @Test @DisplayName("pushToUser - success")
    void pushToUser_success() {
        NotificationResponse response = new NotificationResponse(1L, 100L, "SESSION", "t", "m", false, Instant.now());
        service.pushToUser(100L, response);
        verify(messagingTemplate).convertAndSendToUser(eq("100"), eq("/queue/notifications"), eq(response));
    }

    @Test @DisplayName("pushToUser - exception swallowed")
    void pushToUser_exceptionSwallowed() {
        NotificationResponse response = new NotificationResponse(1L, 100L, "SESSION", "t", "m", false, Instant.now());
        doThrow(new RuntimeException("WebSocket down")).when(messagingTemplate)
                .convertAndSendToUser(anyString(), anyString(), any());
        assertDoesNotThrow(() -> service.pushToUser(100L, response));
    }
}
