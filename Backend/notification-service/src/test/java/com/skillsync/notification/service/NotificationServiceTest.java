package com.skillsync.notification.service;

import com.skillsync.cache.CacheService;
import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.repository.NotificationRepository;
import com.skillsync.notification.service.command.NotificationCommandService;
import com.skillsync.notification.service.query.NotificationQueryService;
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
    @Mock private CacheService cacheService;

    @InjectMocks private NotificationCommandService notificationCommandService;
    @InjectMocks private NotificationQueryService notificationQueryService;

    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testNotification = Notification.builder()
                .id(1L).userId(100L).type("SESSION").title("New Session")
                .message("You have a new session request").isRead(false).build();
    }

    @Test
    @DisplayName("Create and push notification - saves and invalidates unread cache")
    void createAndPush_shouldSaveAndInvalidateCache() {
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        Notification result = notificationCommandService.createAndPush(100L, "SESSION", "New Session", "You have a new session request");

        assertNotNull(result);
        assertEquals("SESSION", result.getType());
        verify(notificationRepository).save(any(Notification.class));
        verify(webSocketService).pushToUser(eq(100L), any(NotificationResponse.class));
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }

    @Test
    @DisplayName("Get unread count - cache miss → DB fetch")
    void getUnreadCount_shouldReturnCountFromDB() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("notification:unread:100")), eq(Long.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Long> fallback = inv.getArgument(3);
                    return fallback.get();
                });
        when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(5L);

        long count = notificationQueryService.getUnreadCount(100L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("Get unread count - cache HIT → NO DB fetch")
    void getUnreadCount_shouldReturnFromCache() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("notification:unread:100")), eq(Long.class), any(), any()))
                .thenReturn(5L);

        long count = notificationQueryService.getUnreadCount(100L);

        assertEquals(5L, count);
        verify(notificationRepository, never()).countByUserIdAndIsReadFalse(anyLong()); // bypass DB
    }

    @Test
    @DisplayName("Mark as read - invalidates unread cache")
    void markAsRead_shouldSetReadTrueAndInvalidateCache() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any())).thenReturn(testNotification);

        notificationCommandService.markAsRead(1L);

        assertTrue(testNotification.isRead());
        verify(notificationRepository).save(testNotification);
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }

    @Test
    @DisplayName("Mark as read - not found throws exception")
    void markAsRead_shouldThrowWhenNotFound() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> notificationCommandService.markAsRead(999L));
    }

    @Test
    @DisplayName("Delete notification - invalidates cache")
    void deleteNotification_shouldCallRepositoryAndInvalidateCache() {
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        notificationCommandService.deleteNotification(1L);
        verify(notificationRepository).deleteById(1L);
        verify(cacheService).evict(CacheService.vKey("notification:unread:100"));
    }
}
