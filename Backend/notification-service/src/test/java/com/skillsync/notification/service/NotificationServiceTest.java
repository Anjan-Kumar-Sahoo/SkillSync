package com.skillsync.notification.service;

import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private WebSocketService webSocketService;
    @InjectMocks private NotificationService service;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L).userId(100L).type("SESSION").title("New Session")
                .message("You have a new session").isRead(false)
                .createdAt(Instant.now()).build();
    }

    @Test @DisplayName("createAndPush - saves and pushes via WebSocket")
    void createAndPush() {
        when(notificationRepository.save(any())).thenReturn(testNotification);
        Notification result = service.createAndPush(100L, "SESSION", "New Session", "msg");
        assertNotNull(result);
        verify(webSocketService).pushToUser(eq(100L), any(NotificationResponse.class));
    }

    @Test @DisplayName("getNotifications")
    void getNotifications() {
        Page<Notification> page = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getNotifications(100L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getUnreadCount")
    void getUnreadCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(5L);
        assertEquals(5L, service.getUnreadCount(100L));
    }

    @Test @DisplayName("markAsRead - success")
    void markAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        service.markAsRead(1L);
        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test @DisplayName("markAsRead - not found throws")
    void markAsRead_notFound() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.markAsRead(1L));
    }

    @Test @DisplayName("markAllAsRead")
    void markAllAsRead() {
        service.markAllAsRead(100L);
        verify(notificationRepository).markAllAsRead(100L);
    }

    @Test @DisplayName("deleteNotification")
    void deleteNotification() {
        service.deleteNotification(1L);
        verify(notificationRepository).deleteById(1L);
    }
}
