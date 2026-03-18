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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private WebSocketService webSocketService;

    @InjectMocks private NotificationService notificationService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L)
                .userId(100L)
                .type("SESSION")
                .title("New Session")
                .message("You have a new session request")
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("Create and push notification - success")
    void createAndPush_shouldSaveAndPush() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationService.createAndPush(100L, "SESSION", "New Session", "You have a new session request");

        assertNotNull(result);
        assertEquals("SESSION", result.getType());
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).pushToUser(eq(100L), any(NotificationResponse.class));
    }

    @Test
    @DisplayName("Get unread count - returns count")
    void getUnreadCount_shouldReturnCount() {
        when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(5L);

        long count = notificationService.getUnreadCount(100L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Mark as read - success")
    void markAsRead_shouldSetReadTrue() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any())).thenReturn(testNotification);

        notificationService.markAsRead(1L);

        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("Mark as read - not found throws exception")
    void markAsRead_shouldThrowWhenNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationService.markAsRead(999L));
    }

    @Test
    @DisplayName("Delete notification - calls repository")
    void deleteNotification_shouldCallRepository() {
        notificationService.deleteNotification(1L);
        verify(notificationRepository).deleteById(1L);
    }
}
