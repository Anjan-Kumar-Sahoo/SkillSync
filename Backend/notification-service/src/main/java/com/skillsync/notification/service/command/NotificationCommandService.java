package com.skillsync.notification.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.mapper.NotificationMapper;
import com.skillsync.notification.repository.NotificationRepository;
import com.skillsync.notification.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * CQRS Command Service for Notification operations.
 * Handles all write operations and cache invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationCommandService {

    private final NotificationRepository notificationRepository;
    private final WebSocketService webSocketService;
    private final CacheService cacheService;

    public Notification createAndPush(Long userId, String type, String title, String message) {
        Notification notification = Notification.builder()
                .userId(userId).type(type).title(title)
            .message(message).isRead(false)
            .createdAt(Instant.now())
            .build();
        notification = notificationRepository.save(notification);

        // Push via WebSocket
        webSocketService.pushToUser(userId, NotificationMapper.toResponse(notification));

        // Invalidate unread count cache
        cacheService.evict(CacheService.vKey("notification:unread:" + userId));

        log.info("[CQRS:COMMAND] Notification sent to user {}. Cache invalidated.", userId);
        return notification;
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setRead(true);
        notificationRepository.save(notification);

        cacheService.evict(CacheService.vKey("notification:unread:" + notification.getUserId()));
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
        cacheService.evict(CacheService.vKey("notification:unread:" + userId));
    }

    @Transactional
    public void deleteNotification(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notificationRepository.delete(notification);
        cacheService.evict(CacheService.vKey("notification:unread:" + userId));
    }

    @Transactional
    public int deleteAllNotifications(Long userId) {
        int deletedCount = notificationRepository.deleteAllByUserId(userId);
        cacheService.evict(CacheService.vKey("notification:unread:" + userId));
        log.info("[CQRS:COMMAND] Deleted {} notifications for user {}", deletedCount, userId);
        return deletedCount;
    }
}
