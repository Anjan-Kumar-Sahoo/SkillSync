package com.skillsync.notification.mapper;

import com.skillsync.notification.dto.NotificationResponse;
import com.skillsync.notification.entity.Notification;

/**
 * Pure mapping functions for Notification entities.
 * Used by both NotificationCommandService and NotificationQueryService (CQRS decoupling).
 */
public final class NotificationMapper {

    private NotificationMapper() {}

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(), notification.getUserId(), notification.getType(),
                notification.getTitle(), notification.getMessage(),
                notification.isRead(), notification.getCreatedAt()
        );
    }
}
