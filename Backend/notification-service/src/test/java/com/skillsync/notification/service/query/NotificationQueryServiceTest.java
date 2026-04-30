package com.skillsync.notification.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.repository.NotificationRepository;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private CacheService cacheService;
    @InjectMocks private NotificationQueryService service;

    @Test @DisplayName("getNotifications")
    void getNotifications() {
        Notification n = Notification.builder().id(1L).userId(100L).type("SESSION")
                .title("t").message("m").isRead(false).createdAt(Instant.now()).build();
        Page<Notification> page = new PageImpl<>(List.of(n));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(100L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getNotifications(100L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getUnreadCount - cached value")
    void getUnreadCount_cached() {
        when(cacheService.getOrLoad(anyString(), eq(Long.class), any(), any())).thenReturn(5L);
        assertEquals(5L, service.getUnreadCount(100L));
    }

    @Test @DisplayName("getUnreadCount - null cached returns 0")
    void getUnreadCount_nullCached() {
        when(cacheService.getOrLoad(anyString(), eq(Long.class), any(), any())).thenReturn(null);
        assertEquals(0L, service.getUnreadCount(100L));
    }

    @Test @DisplayName("getUnreadCount - cache miss loads from DB")
    void getUnreadCount_cacheMiss() {
        when(cacheService.getOrLoad(anyString(), eq(Long.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<Long> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(notificationRepository.countByUserIdAndIsReadFalse(100L)).thenReturn(3L);
        assertEquals(3L, service.getUnreadCount(100L));
    }
}
