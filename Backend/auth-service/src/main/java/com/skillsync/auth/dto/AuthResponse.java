package com.skillsync.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AuthResponse(
        @JsonIgnore String accessToken,
        @JsonIgnore String refreshToken,
        long expiresIn,
        String tokenType,
        UserSummary user
) {}
