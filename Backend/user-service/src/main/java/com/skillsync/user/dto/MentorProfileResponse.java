package com.skillsync.user.dto;

import java.math.BigDecimal;
import java.util.List;

public record MentorProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String avatarUrl,
        String bio,
        int experienceYears,
        BigDecimal hourlyRate,
        double avgRating,
        int totalReviews,
        int totalSessions,
        String status,
        List<SkillSummary> skills,
        List<AvailabilitySlotResponse> availability
) {}
