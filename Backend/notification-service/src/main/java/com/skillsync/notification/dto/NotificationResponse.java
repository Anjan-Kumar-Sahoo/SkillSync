package com.skillsync.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        String type,
        String title,
        String message,
        boolean isRead,
        Instant createdAt
) {}
