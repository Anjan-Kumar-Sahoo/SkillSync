package com.skillsync.auth.service;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.RefreshToken;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.RefreshTokenRepository;
import com.skillsync.auth.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthUserRepository authUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final OtpService otpService;

    private static final int MAX_REFRESH_TOKENS = 5;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already registered: " + request.email());
        }

        AuthUser user = AuthUser.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(Role.ROLE_LEARNER)
                .isActive(true)
                .isVerified(false)
                .build();

        user = authUserRepository.save(user);
        log.info("User registered successfully: {}", user.getEmail());

        // Send OTP for email verification
        otpService.generateAndSendOtp(user);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token expired");
        }

        AuthUser user = refreshToken.getUser();
        refreshTokenRepository.delete(refreshToken);

        log.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken)
                .ifPresent(refreshTokenRepository::delete);
        log.info("User logged out");
    }

    @Transactional
    public void updateUserRole(Long userId, String role) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        user.setRole(Role.valueOf(role));
        authUserRepository.save(user);
        log.info("User role updated to {} for userId: {}", role, userId);
    }

    private AuthResponse generateAuthResponse(AuthUser user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        // Enforce max refresh tokens per user (FIFO eviction)
        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserOrderByCreatedAtAsc(user);
        if (existingTokens.size() >= MAX_REFRESH_TOKENS) {
            refreshTokenRepository.delete(existingTokens.get(0));
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenStr)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        refreshTokenRepository.save(refreshToken);

        UserSummary userSummary = new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName()
        );

        return new AuthResponse(
                accessToken,
                refreshTokenStr,
                jwtTokenProvider.getAccessExpiration() / 1000,
                "Bearer",
                userSummary
        );
    }
}
