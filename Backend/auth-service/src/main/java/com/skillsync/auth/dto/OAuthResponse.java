package com.skillsync.auth.dto;

/**
 * OAuth login response that includes password setup requirement flag.
 * When {@code passwordSetupRequired} is true, the frontend MUST redirect
 * the user to the password setup screen before allowing full access.
 */
public record OAuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        String tokenType,
        UserSummary user,
        boolean passwordSetupRequired
) {}
