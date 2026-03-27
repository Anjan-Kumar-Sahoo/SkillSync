package com.skillsync.auth.dto;

public record UserSummary(
        Long id,
        String email,
        String role,
        String firstName,
        String lastName
) {}
