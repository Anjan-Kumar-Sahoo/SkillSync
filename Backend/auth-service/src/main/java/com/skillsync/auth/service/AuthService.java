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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private final EmailService emailService;
    private final CacheService cacheService;

    private static final int MAX_REFRESH_TOKENS = 5;
    private static final Pattern STRONG_PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,100}$"
    );

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Legacy support
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

        otpService.generateAndSendOtp(user, OtpType.REGISTRATION);

        return generateAuthResponse(user);
    }

    @Transactional
    public Map<String, Object> initiateRegistration(InitiateRegistrationRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<AuthUser> existingUserOpt = authUserRepository.findByEmail(normalizedEmail);
        if (existingUserOpt.isPresent()) {
            AuthUser existing = existingUserOpt.get();
            if (existing.isVerified()) {
                return Map.of("exists", true, "message", "User already registered.");
            } else {
                otpService.generateAndSendOtp(existing, OtpType.REGISTRATION);
                return Map.of("exists", false, "otpSent", true);
            }
        }

        AuthUser user = AuthUser.builder()
                .email(normalizedEmail)
                .passwordHash("PENDING")
                .firstName("PENDING")
                .lastName("PENDING")
                .role(Role.ROLE_LEARNER)
                .isActive(true)
                .isVerified(false)
                .passwordSet(false)
                .build();

        user = authUserRepository.save(user);
        otpService.generateAndSendOtp(user, OtpType.REGISTRATION);

        return Map.of("exists", false, "otpSent", true);
    }

    @Transactional
    public AuthResponse completeRegistration(CompleteRegistrationRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isVerified()) {
            throw new RuntimeException("Email not verified. Please verify OTP first.");
        }

        if (user.isPasswordSet() && !user.getPasswordHash().equals("PENDING")) {
            throw new RuntimeException("Registration already completed for this user.");
        }

        validateStrongPassword(request.password());

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordSet(true);
        user = authUserRepository.save(user);
        
        log.info("User registration completed for: {}", user.getEmail());

        try {
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName());
        } catch (Exception ex) {
            log.warn("Failed to dispatch welcome email to {}: {}", user.getEmail(), ex.getMessage());
        }

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Email verification is mandatory — block login for unverified users
        if (!user.isVerified()) {
            log.warn("Login attempt by unverified user: {}", user.getEmail());
            // Auto-resend OTP so the user can verify immediately
            otpService.generateAndSendOtp(user, OtpType.REGISTRATION);
            throw new RuntimeException("Email not verified. A new OTP has been sent to " + user.getEmail()
                    + ". Please verify your email before logging in.");
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
        
        // Invalidate Redis profile cache so front-end gets updated role
        cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));
        
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
    public void verifyPasswordResetOtp(String email, String otp) {
        otpService.validateOtp(email, otp, OtpType.PASSWORD_RESET);
        log.info("Password reset OTP validated for: {}", email);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validateStrongPassword(request.newPassword());

        otpService.verifyOtp(request.email(), request.otp(), OtpType.PASSWORD_RESET);

        AuthUser user = authUserRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found with email: " + request.email()));

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new RuntimeException("New password must be different from current password");
        }

        applyPasswordUpdate(user, request.newPassword());
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
            // Mark user as verified if they weren't already (OAuth provider has verified
            // the email)
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
        boolean needsPasswordSetup = isNewUser;

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

        validateStrongPassword(request.password());

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordSet(true);
        authUserRepository.save(user);

        // Invalidate cache
        cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));

        log.info("Password setup completed for OAuth user: {}", user.getEmail());
    }

    private void validateStrongPassword(String password) {
        if (password == null || password.isBlank()) {
            throw new RuntimeException("Password is mandatory");
        }

        if (!STRONG_PASSWORD_PATTERN.matcher(password).matches()) {
            throw new RuntimeException(
                    "Password must be 8-100 chars and include uppercase, lowercase, number, and special character"
            );
        }
    }

    private void applyPasswordUpdate(AuthUser user, String newPassword) {
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordSet(true);
        authUserRepository.save(user);

        refreshTokenRepository.deleteByUser(user);
        cacheService.evict(CacheService.vKey("user:profile:" + user.getId()));
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
                user.getLastName());

        return new AuthResponse(
                accessToken,
                refreshTokenStr,
                jwtTokenProvider.getAccessExpiration() / 1000,
                "Bearer",
                userSummary);
    }

    public UserSummary getUserById(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return new UserSummary(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getFirstName(),
                user.getLastName());
    }

    public Map<String, Object> getAllUsers(int page, int size) {
        return getAllUsersFiltered(page, size, null, null);
    }

    public Map<String, Object> getAllUsersFiltered(int page, int size, String role, String search) {
        var pageable = org.springframework.data.domain.PageRequest.of(
                page,
                size,
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.ASC, "id")
        );
        Role roleEnum = null;
        if (role != null && !role.isBlank()) {
            try { roleEnum = Role.valueOf(role); } catch (IllegalArgumentException ignored) {}
        }
        var usersPage = authUserRepository.findByFilters(roleEnum, search, pageable);
        var content = usersPage.getContent().stream()
                .map(u -> new UserSummary(u.getId(), u.getEmail(), u.getRole().name(),
                        u.getFirstName(), u.getLastName()))
                .toList();
        return Map.of(
                "content", content,
                "totalElements", usersPage.getTotalElements(),
                "totalPages", usersPage.getTotalPages(),
                "number", usersPage.getNumber()
        );
    }

    public long getUserCount(String role) {
        if (role != null && !role.isBlank()) {
            return authUserRepository.countByRole(Role.valueOf(role));
        }
        return authUserRepository.count();
    }

    @Transactional
    public void deleteUser(Long userId) {
        AuthUser user = authUserRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        refreshTokenRepository.deleteByUser(user);
        authUserRepository.delete(user);
        log.info("User deleted: id={}, email={}", userId, user.getEmail());
    }
}
