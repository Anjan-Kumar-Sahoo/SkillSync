package com.skillsync.auth.controller;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.service.AuthService;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${auth.cookie.domain:}")
    private String configuredCookieDomain;

    @Value("${auth.cookie.same-site:}")
    private String configuredCookieSameSite;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response,
                                                 HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.register(request);
        addAuthCookies(response, authResponse, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/initiate-registration")
    public ResponseEntity<?> initiateRegistration(@Valid @RequestBody InitiateRegistrationRequest request) {
        return ResponseEntity.ok(authService.initiateRegistration(request));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request,
                                                             HttpServletResponse response,
                                                             HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.completeRegistration(request);
        addAuthCookies(response, authResponse, httpRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, String>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        otpService.verifyOtp(request.email(), request.otp(), OtpType.REGISTRATION);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<Map<String, String>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        otpService.resendOtp(request.email());
        return ResponseEntity.ok(Map.of("message", "OTP sent successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response,
                                              HttpServletRequest httpRequest) {
        AuthResponse authResponse = authService.login(request);
        addAuthCookies(response, authResponse, httpRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
                                                     @RequestBody(required = false) RefreshTokenRequest refreshTokenBody,
                                                     HttpServletResponse response,
                                                     HttpServletRequest httpRequest) {
        String refreshToken = StringUtils.hasText(refreshTokenCookie)
                ? refreshTokenCookie
                : (refreshTokenBody != null ? refreshTokenBody.refreshToken() : null);

        if (!StringUtils.hasText(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        AuthResponse authResponse = authService.refreshToken(new RefreshTokenRequest(refreshToken));
        addAuthCookies(response, authResponse, httpRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie,
                                       HttpServletResponse response,
                                       HttpServletRequest httpRequest) {
        if (refreshTokenCookie != null) {
            authService.logout(refreshTokenCookie);
        }
        clearAuthCookies(response, httpRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok("Token is valid");
    }

    /**
     * Returns the authenticated user's identity from the JWT cookie.
     * Used by the frontend AuthLoader on page refresh to restore role state.
     */
    @GetMapping("/me")
    public ResponseEntity<UserSummary> getCurrentUser(
            @CookieValue(value = "accessToken", required = false) String accessToken,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else if (accessToken != null) {
            token = accessToken;
        }
        if (token == null || !jwtTokenProvider.isTokenValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = jwtTokenProvider.extractUserId(token);
        UserSummary user = authService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/internal/users/{id}")
    public ResponseEntity<UserSummary> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @GetMapping("/internal/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(authService.getAllUsersFiltered(page, size, role, search));
    }

    @GetMapping("/internal/users/count")
    public ResponseEntity<Long> getUserCount(@RequestParam(required = false) String role) {
        return ResponseEntity.ok(authService.getUserCount(role));
    }

    @DeleteMapping("/internal/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        authService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset OTP sent to email"));
    }

    @PostMapping("/verify-password-reset-otp")
    public ResponseEntity<Map<String, String>> verifyPasswordResetOtp(
            @Valid @RequestBody VerifyPasswordResetOtpRequest request) {
        authService.verifyPasswordResetOtp(request.email(), request.otp());
        return ResponseEntity.ok(Map.of("message", "OTP verified successfully"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/oauth-login")
        public ResponseEntity<OAuthResponse> oauthLogin(@Valid @RequestBody OAuthRequest request,
                                HttpServletResponse response,
                                HttpServletRequest httpRequest) {
        OAuthResponse oauthResponse = authService.loginWithOAuth(request);

        addAuthCookies(
            response,
            oauthResponse.accessToken(),
            oauthResponse.refreshToken(),
            oauthResponse.expiresIn(),
            httpRequest
        );
        
        return ResponseEntity.ok(oauthResponse);
    }

    @PostMapping("/setup-password")
    public ResponseEntity<Map<String, String>> setupPassword(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                                             @CookieValue(value = "accessToken", required = false) String accessToken,
                                                             @Valid @RequestBody SetupPasswordRequest request) {
        String token = (authHeader != null && authHeader.startsWith("Bearer ")) ? authHeader.substring(7) : accessToken;
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = jwtTokenProvider.extractEmail(token);
        authService.setupPassword(new SetupPasswordRequest(email, request.password()));
        return ResponseEntity
                .ok(Map.of("message", "Password set successfully. You can now login with email and password."));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateUserRole(@PathVariable Long id, @RequestParam String role) {
        authService.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/internal/users/{id}/role")
    public ResponseEntity<Void> updateUserRoleInternal(@PathVariable Long id, @RequestParam String role) {
        authService.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    private void addAuthCookies(HttpServletResponse response, AuthResponse authResponse, HttpServletRequest request) {
        addAuthCookies(
                response,
                authResponse.accessToken(),
                authResponse.refreshToken(),
                authResponse.expiresIn(),
                request
        );
    }

    private void addAuthCookies(HttpServletResponse response,
                                String accessToken,
                                String refreshToken,
                                long accessTokenMaxAgeSeconds,
                                HttpServletRequest request) {
        CookieOptions cookieOptions = resolveCookieOptions(request);
        long refreshTokenMaxAgeSeconds = Math.max(1, jwtTokenProvider.getRefreshExpiration() / 1000);

        String accessTokenCookie = buildCookie("accessToken", accessToken, accessTokenMaxAgeSeconds, cookieOptions);
        String refreshTokenCookie = buildCookie("refreshToken", refreshToken, refreshTokenMaxAgeSeconds, cookieOptions);

        response.addHeader("Set-Cookie", accessTokenCookie);
        response.addHeader("Set-Cookie", refreshTokenCookie);
    }

    private void clearAuthCookies(HttpServletResponse response, HttpServletRequest request) {
        CookieOptions cookieOptions = resolveCookieOptions(request);

        String accessCookie = buildCookie("accessToken", "", 0, cookieOptions);
        String refreshCookie = buildCookie("refreshToken", "", 0, cookieOptions);
        
        response.addHeader("Set-Cookie", accessCookie);
        response.addHeader("Set-Cookie", refreshCookie);
    }

    private CookieOptions resolveCookieOptions(HttpServletRequest request) {
        String serverHost = normalizeHost(request.getServerName());
        String forwardedHost = extractForwardedHost(request.getHeader("X-Forwarded-Host"));
        String originHost = extractOriginHost(request.getHeader("Origin"));

        boolean isLocalHost = isLocalHost(serverHost)
            || isLocalHost(forwardedHost)
            || isLocalHost(originHost);

        boolean secure = !isLocalHost;
        String sameSite = StringUtils.hasText(configuredCookieSameSite)
                ? configuredCookieSameSite
                : (secure ? "None" : "Lax");
        String domain = (!isLocalHost && StringUtils.hasText(configuredCookieDomain))
            ? configuredCookieDomain.trim()
            : null;

        return new CookieOptions(secure, sameSite, domain);
    }

    private String buildCookie(String cookieName, String value, long maxAgeSeconds, CookieOptions cookieOptions) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(cookieOptions.secure())
                .sameSite(cookieOptions.sameSite())
                .path("/")
                .maxAge(maxAgeSeconds);

        if (StringUtils.hasText(cookieOptions.domain())) {
            builder.domain(cookieOptions.domain());
        }

        return builder.build().toString();
    }

    private boolean isLocalHost(String host) {
        if (!StringUtils.hasText(host)) {
            return false;
        }

        String normalized = normalizeHost(host);
        if (!StringUtils.hasText(normalized)) {
            return false;
        }

        return "localhost".equals(normalized)
                || "127.0.0.1".equals(normalized)
                || "::1".equals(normalized);
    }

    private String extractForwardedHost(String forwardedHostHeader) {
        if (!StringUtils.hasText(forwardedHostHeader)) {
            return null;
        }

        String first = forwardedHostHeader.split(",")[0].trim();
        return normalizeHost(first);
    }

    private String extractOriginHost(String originHeader) {
        if (!StringUtils.hasText(originHeader)) {
            return null;
        }

        try {
            return normalizeHost(URI.create(originHeader).getHost());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String normalizeHost(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }

        String normalized = host.trim().toLowerCase();

        if (normalized.startsWith("[") && normalized.contains("]")) {
            return normalized.substring(1, normalized.indexOf(']'));
        }

        int colonIdx = normalized.indexOf(':');
        if (colonIdx > -1) {
            return normalized.substring(0, colonIdx);
        }

        return normalized;
    }

    private record CookieOptions(boolean secure, String sameSite, String domain) {}
}
