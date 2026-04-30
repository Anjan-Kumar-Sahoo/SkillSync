package com.skillsync.notification.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.repository.NotificationRepository;
import com.skillsync.notification.service.WebSocketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private WebSocketService webSocketService;
    @Mock private CacheService cacheService;
    @InjectMocks private NotificationCommandService service;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L).userId(100L).type("SESSION").title("New")
                .message("msg").isRead(false).createdAt(Instant.now()).build();
    }

    @Test @DisplayName("createAndPush - saves, pushes, invalidates cache")
    void createAndPush() {
        when(notificationRepository.save(any())).thenReturn(testNotification);
        Notification result = service.createAndPush(100L, "SESSION", "New", "msg");
        assertNotNull(result);
        verify(webSocketService).pushToUser(eq(100L), any());
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }

    @Test @DisplayName("markAsRead - success")
    void markAsRead() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        service.markAsRead(1L);
        assertTrue(testNotification.isRead());
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
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
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }

    @Test @DisplayName("deleteNotification - success")
    void deleteNotification() {
        when(notificationRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.of(testNotification));
        service.deleteNotification(100L, 1L);
        verify(notificationRepository).delete(testNotification);
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }

    @Test @DisplayName("deleteNotification - not found throws")
    void deleteNotification_notFound() {
        when(notificationRepository.findByIdAndUserId(1L, 100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.deleteNotification(100L, 1L));
    }

    @Test @DisplayName("deleteAllNotifications")
    void deleteAllNotifications() {
        when(notificationRepository.deleteAllByUserId(100L)).thenReturn(5);
        int count = service.deleteAllNotifications(100L);
        assertEquals(5, count);
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }
}
