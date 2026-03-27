package com.skillsync.user.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProfileResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String email,
        String bio,
        String avatarUrl,
        String phone,
        String location,
        int profileCompletePct,
        List<SkillSummary> skills,
        LocalDateTime createdAt
) {}
