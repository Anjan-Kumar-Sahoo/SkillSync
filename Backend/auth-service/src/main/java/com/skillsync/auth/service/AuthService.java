package com.skillsync.auth.service;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.RefreshToken;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.RefreshTokenRepository;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.cache.CacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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
    private final CacheService cacheService;

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
        otpService.generateAndSendOtp(user, OtpType.REGISTRATION);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Email verification is mandatory — block login for unverified users
        if (!user.isVerified()) {
            log.warn("Login attempt by unverified user: {}", user.getEmail());
            // Auto-resend OTP so the user can verify immediately
            otpService.generateAndSendOtp(user, OtpType.REGISTRATION);
            throw new RuntimeException("Email not verified. A new OTP has been sent to " + user.getEmail() + ". Please verify your email before logging in.");
        }

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

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.email()));

        otpService.generateAndSendOtp(user, OtpType.PASSWORD_RESET);
        log.info("Password reset OTP sent to: {}", request.email());
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        otpService.verifyOtp(request.email(), request.otp(), OtpType.PASSWORD_RESET);

        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.email()));

        // Update password
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        authUserRepository.save(user);

        // Invalidate all refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Invalidate Redis profile cache in user-service
        cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));

        log.info("Password reset successfully for user: {}", request.email());
    }

    @Transactional
    public OAuthResponse loginWithOAuth(OAuthRequest request) {
        // 1. Try to find user by provider+providerId first, then by email
        AuthUser user = authUserRepository.findByProviderAndProviderId(request.provider(), request.providerId())
                .or(() -> authUserRepository.findByEmail(request.email()))
                .orElse(null);

        boolean isNewUser = false;

        if (user == null) {
            // === NEW USER: Create via OAuth ===
            AuthUser newUser = AuthUser.builder()
                    .email(request.email())
                    .firstName(request.firstName())
                    .lastName(request.lastName())
                    .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString())) // Placeholder
                    .role(Role.ROLE_LEARNER)
                    .isActive(true)
                    .isVerified(true) // Verified by OAuth provider
                    .passwordSet(false) // MUST set password before full access
                    .provider(request.provider())
                    .providerId(request.providerId())
                    .build();
            user = authUserRepository.save(newUser);
            isNewUser = true;
            log.info("OAuth new user created: {} via {}", user.getEmail(), request.provider());
        } else {
            // === EXISTING USER ===
            // Mark user as verified if they weren't already (OAuth provider has verified the email)
            if (!user.isVerified()) {
                user.setVerified(true);
                log.info("OAuth login verified previously unverified user: {}", user.getEmail());
            }

            // Link OAuth provider if user existed but was local-only
            if (user.getProvider() == null) {
                user.setProvider(request.provider());
                user.setProviderId(request.providerId());
                authUserRepository.save(user);
                cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));
                log.info("OAuth provider linked for existing user: {} → {}", user.getEmail(), request.provider());
            }
        }

        log.info("OAuth login successful for: {} via {}", user.getEmail(), user.getProvider());

        // Generate tokens
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshTokenStr = jwtTokenProvider.generateRefreshToken(user.getId());

        List<RefreshToken> existingTokens = refreshTokenRepository.findByUserOrderByCreatedAtAsc(user);
        if (existingTokens.size() >= MAX_REFRESH_TOKENS) {
            refreshTokenRepository.delete(existingTokens.get(0));
        }

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user).token(refreshTokenStr)
                .expiresAt(LocalDateTime.now().plusDays(7)).build();
        refreshTokenRepository.save(refreshToken);

        UserSummary userSummary = new UserSummary(user.getId(), user.getEmail(),
                user.getRole().name(), user.getFirstName(), user.getLastName());

        // For new users: passwordSetupRequired=true, existing users: false
        boolean needsPasswordSetup = isNewUser || !user.isPasswordSet();

        return new OAuthResponse(accessToken, refreshTokenStr,
                jwtTokenProvider.getAccessExpiration() / 1000, "Bearer",
                userSummary, needsPasswordSetup);
    }

    /**
     * Setup password for OAuth users who registered without one.
     * This is a mandatory step before full access is granted.
     */
    @Transactional
    public void setupPassword(SetupPasswordRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.email()));

        if (user.isPasswordSet()) {
            throw new RuntimeException("Password is already set for this account.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordSet(true);
        authUserRepository.save(user);

        // Invalidate cache
        cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));

        log.info("Password setup completed for OAuth user: {}", user.getEmail());
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

    public UserSummary getUserById(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName()
        );
    }
}
