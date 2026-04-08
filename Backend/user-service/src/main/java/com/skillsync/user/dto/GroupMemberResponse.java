package com.skillsync.user.dto;

import java.time.LocalDateTime;

public record GroupMemberResponse(
        Long id,
        Long userId,
        String name,
        String email,
        String role,
        LocalDateTime joinedAt
) {}
