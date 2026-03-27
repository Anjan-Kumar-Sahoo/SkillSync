package com.skillsync.session.dto;

import java.time.LocalDateTime;

public record SessionResponse(
        Long id, Long mentorId, Long learnerId,
        String topic, String description,
        LocalDateTime sessionDate, int durationMinutes,
        String meetingLink, String status,
        String cancelReason, LocalDateTime createdAt
) {}
