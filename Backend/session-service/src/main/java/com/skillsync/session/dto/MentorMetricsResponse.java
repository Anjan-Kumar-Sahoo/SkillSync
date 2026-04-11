package com.skillsync.session.dto;

public record MentorMetricsResponse(
        Long mentorId,
        long completedSessions,
        double averageRating,
        int totalReviews,
        long defaultRatedSessions,
        boolean newMentor
) {}
