package com.skillsync.auth.controller;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.service.AuthService;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpService otpService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.register(request);
        addAuthCookies(response, authResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(authResponse);
    }

    @PostMapping("/initiate-registration")
    public ResponseEntity<?> initiateRegistration(@Valid @RequestBody InitiateRegistrationRequest request) {
        return ResponseEntity.ok(authService.initiateRegistration(request));
    }

    @PostMapping("/complete-registration")
    public ResponseEntity<AuthResponse> completeRegistration(@Valid @RequestBody CompleteRegistrationRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.completeRegistration(request);
        addAuthCookies(response, authResponse);
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
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        addAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie, HttpServletResponse response) {
        if (refreshTokenCookie == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        AuthResponse authResponse = authService.refreshToken(new RefreshTokenRequest(refreshTokenCookie));
        addAuthCookies(response, authResponse);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refreshToken", required = false) String refreshTokenCookie, HttpServletResponse response) {
        if (refreshTokenCookie != null) {
            authService.logout(refreshTokenCookie);
        }
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/validate")
    public ResponseEntity<String> validateToken(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok("Token is valid");
    }

    @GetMapping("/internal/users/{id}")
    public ResponseEntity<UserSummary> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset OTP sent to email"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully"));
    }

    @PostMapping("/oauth-login")
    public ResponseEntity<OAuthResponse> oauthLogin(@Valid @RequestBody OAuthRequest request, HttpServletResponse response) {
        OAuthResponse oauthResponse = authService.loginWithOAuth(request);
        
        // Use exactly the same cookies setup for the OAuthResponse token structures
        String accessTokenCookie = ResponseCookie.from("accessToken", oauthResponse.accessToken())
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/").maxAge(oauthResponse.expiresIn()).build().toString();
        String refreshTokenCookie = ResponseCookie.from("refreshToken", oauthResponse.refreshToken())
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/").maxAge(7 * 24 * 60 * 60).build().toString();
        
        response.addHeader("Set-Cookie", accessTokenCookie);
        response.addHeader("Set-Cookie", refreshTokenCookie);
        
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

    private void addAuthCookies(HttpServletResponse response, AuthResponse authResponse) {
        String accessTokenCookie = ResponseCookie.from("accessToken", authResponse.accessToken())
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/")
                .maxAge(authResponse.expiresIn()).build().toString();
        String refreshTokenCookie = ResponseCookie.from("refreshToken", authResponse.refreshToken())
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/")
                .maxAge(7 * 24 * 60 * 60).build().toString();

        response.addHeader("Set-Cookie", accessTokenCookie);
        response.addHeader("Set-Cookie", refreshTokenCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        String accessCookie = ResponseCookie.from("accessToken", "")
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/").maxAge(0).build().toString();
        String refreshCookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true).secure(true).sameSite("None").domain(".mraks.dev").path("/").maxAge(0).build().toString();
        
        response.addHeader("Set-Cookie", accessCookie);
        response.addHeader("Set-Cookie", refreshCookie);
    }
}
