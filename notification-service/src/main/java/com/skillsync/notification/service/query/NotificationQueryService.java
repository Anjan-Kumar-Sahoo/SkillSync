package com.skillsync.notification.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;
import com.skillsync.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * CQRS Query Service for Notification operations.
 * Caches unread count with stampede protection (short TTL: 2 minutes).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final CacheService cacheService;

    @Value("${cache.ttl.notification:120}")
    private long notificationTtl;

    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationQueryService::mapToResponse);
    }

    /**
     * Cache-aside with stampede protection for unread count.
     */
    public long getUnreadCount(Long userId) {
        String cacheKey = CacheService.vKey("notification:unread:" + userId);

        Long cached = cacheService.getOrLoad(cacheKey, Long.class,
                Duration.ofSeconds(notificationTtl), () ->
                        notificationRepository.countByUserIdAndIsReadFalse(userId));
        return cached != null ? cached : 0L;
    }

    /**
     * Shared mapper — also used by NotificationCommandService.
     */
    public static NotificationResponse mapToResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getUserId(), n.getType(),
                n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt());
    }
}
