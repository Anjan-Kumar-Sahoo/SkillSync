package com.skillsync.notification.dto;

public record UserSummary(
    Long id,
    String email,
    String role,
    String firstName,
    String lastName
) {}
