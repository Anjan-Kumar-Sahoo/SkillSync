package com.skillsync.user.dto;

public record AdminStatsResponse(
        long totalUsers,
        long totalMentors,
        long totalSessions,
        long pendingMentorApprovals
) {}
