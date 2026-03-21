package com.skillsync.session.review.dto;

import java.time.LocalDateTime;

public record ReviewResponse(Long id, Long sessionId, Long mentorId, Long reviewerId,
    int rating, String comment, LocalDateTime createdAt) {}
