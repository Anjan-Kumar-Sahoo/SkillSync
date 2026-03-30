package com.skillsync.auth.service;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.RefreshTokenRepository;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.cache.CacheService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;
    @Mock private CacheService cacheService;

    @InjectMocks private AuthService authService;

    private AuthUser testUser;
    private AuthUser verifiedUser;

    @BeforeEach
    void setUp() {
        testUser = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_LEARNER)
                .isActive(true)
                .isVerified(false)
                .passwordSet(true)
                .build();

        verifiedUser = AuthUser.builder()
                .id(2L)
                .email("verified@example.com")
                .passwordHash("encodedPassword")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.ROLE_LEARNER)
                .isActive(true)
                .isVerified(true)
                .passwordSet(true)
                .build();
    }

    @Test
    @DisplayName("Register - success and OTP is sent")
    void register_shouldCreateUserAndSendOtp() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");

        when(authUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessExpiration()).thenReturn(3600000L);
        when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
        when(refreshTokenRepository.save(any())).thenReturn(null);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        assertEquals("refreshToken", response.refreshToken());
        verify(authUserRepository).save(any(AuthUser.class));
        verify(otpService, times(1)).generateAndSendOtp(any(AuthUser.class), eq(OtpType.REGISTRATION));
    }

    @Test
    @DisplayName("Register - duplicate email throws exception")
    void register_shouldThrowExceptionForDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        when(authUserRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(authUserRepository, never()).save(any());
        verify(otpService, never()).generateAndSendOtp(any(), any());
    }

    @Test
    @DisplayName("Login - success with verified user")
    void login_shouldAuthenticateVerifiedUserAndReturnToken() {
        LoginRequest request = new LoginRequest("verified@example.com", "password123");

        when(authUserRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
        when(jwtTokenProvider.getAccessExpiration()).thenReturn(3600000L);
        when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
        when(refreshTokenRepository.save(any())).thenReturn(null);

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("accessToken", response.accessToken());
        verify(authenticationManager).authenticate(any());
        verify(otpService, never()).generateAndSendOtp(any(), any());
    }

    @Test
    @DisplayName("Login - unverified user is blocked and OTP is re-sent")
    void login_shouldRejectUnverifiedUserAndResendOtp() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Email not verified"));
        assertTrue(ex.getMessage().contains("A new OTP has been sent"));
        verify(otpService, times(1)).generateAndSendOtp(any(AuthUser.class), eq(OtpType.REGISTRATION));
    }

    @Test
    @DisplayName("Login - user not found throws exception")
    void login_shouldThrowExceptionWhenUserNotFound() {
        LoginRequest request = new LoginRequest("unknown@example.com", "password123");
        when(authUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> authService.login(request));
    }

    @Test
    @DisplayName("Logout - deletes refresh token")
    void logout_shouldDeleteRefreshToken() {
        authService.logout("someRefreshToken");
        verify(refreshTokenRepository).findByToken("someRefreshToken");
    }

    @Test
    @DisplayName("Update user role - success")
    void updateUserRole_shouldUpdateRole() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(authUserRepository.save(any())).thenReturn(testUser);

        authService.updateUserRole(1L, "ROLE_MENTOR");

        assertEquals(Role.ROLE_MENTOR, testUser.getRole());
        verify(authUserRepository).save(testUser);
    }

    // =========================================================================
    // OAuth Flow Tests
    // =========================================================================
    @Nested
    @DisplayName("OAuth Flow Tests")
    class OAuthFlowTests {

        @Test
        @DisplayName("OAuth - New user → creates account with passwordSetupRequired=true")
        void oauth_newUser_shouldCreateAndRequirePasswordSetup() {
            OAuthRequest request = new OAuthRequest(
                    "newuser@gmail.com", "Google", "User", "google", "google-id-123");

            when(authUserRepository.findByProviderAndProviderId("google", "google-id-123"))
                    .thenReturn(Optional.empty());
            when(authUserRepository.findByEmail("newuser@gmail.com"))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedPlaceholder");
            AuthUser newUser = AuthUser.builder()
                    .id(10L).email("newuser@gmail.com").firstName("Google").lastName("User")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(true).passwordSet(false)
                    .provider("google").providerId("google-id-123").build();
            when(authUserRepository.save(any(AuthUser.class))).thenReturn(newUser);
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
            when(jwtTokenProvider.getAccessExpiration()).thenReturn(3600000L);
            when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
            when(refreshTokenRepository.save(any())).thenReturn(null);

            OAuthResponse response = authService.loginWithOAuth(request);

            assertNotNull(response);
            assertTrue(response.passwordSetupRequired(), "New OAuth user should require password setup");
            assertEquals("accessToken", response.accessToken());
            verify(authUserRepository).save(any(AuthUser.class));
        }

        @Test
        @DisplayName("OAuth - Existing verified user → direct login, no password prompt")
        void oauth_existingVerifiedUser_shouldLoginDirectly() {
            OAuthRequest request = new OAuthRequest(
                    "verified@example.com", "Jane", "Doe", "google", "google-id-456");

            AuthUser existingUser = AuthUser.builder()
                    .id(2L).email("verified@example.com").firstName("Jane").lastName("Doe")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(true).passwordSet(true)
                    .provider("google").providerId("google-id-456").build();

            when(authUserRepository.findByProviderAndProviderId("google", "google-id-456"))
                    .thenReturn(Optional.of(existingUser));
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
            when(jwtTokenProvider.getAccessExpiration()).thenReturn(3600000L);
            when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());
            when(refreshTokenRepository.save(any())).thenReturn(null);

            OAuthResponse response = authService.loginWithOAuth(request);

            assertNotNull(response);
            assertFalse(response.passwordSetupRequired(), "Existing verified user should NOT require password setup");
            assertEquals("accessToken", response.accessToken());
            // Should NOT create a new user
            verify(authUserRepository, never()).save(any(AuthUser.class));
        }

        @Test
        @DisplayName("OAuth - Unverified user → blocked from OAuth login")
        void oauth_unverifiedUser_shouldBeBlocked() {
            OAuthRequest request = new OAuthRequest(
                    "unverified@example.com", "Unverified", "User", "google", "google-id-789");

            AuthUser unverifiedUser = AuthUser.builder()
                    .id(3L).email("unverified@example.com").firstName("Unverified").lastName("User")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(false).passwordSet(true).build();

            when(authUserRepository.findByProviderAndProviderId("google", "google-id-789"))
                    .thenReturn(Optional.empty());
            when(authUserRepository.findByEmail("unverified@example.com"))
                    .thenReturn(Optional.of(unverifiedUser));

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.loginWithOAuth(request));

            assertTrue(ex.getMessage().contains("not verified"));
            verify(authUserRepository, never()).save(any(AuthUser.class));
        }

        @Test
        @DisplayName("OAuth - Setup password for new OAuth user")
        void setupPassword_shouldSetPasswordForOAuthUser() {
            AuthUser oauthUser = AuthUser.builder()
                    .id(10L).email("oauth@gmail.com").firstName("OAuth").lastName("User")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(true).passwordSet(false)
                    .provider("google").providerId("google-id-123").build();

            when(authUserRepository.findByEmail("oauth@gmail.com")).thenReturn(Optional.of(oauthUser));
            when(passwordEncoder.encode("myNewPassword")).thenReturn("encodedNewPassword");
            when(authUserRepository.save(any())).thenReturn(oauthUser);

            SetupPasswordRequest request = new SetupPasswordRequest("oauth@gmail.com", "myNewPassword");
            authService.setupPassword(request);

            assertTrue(oauthUser.isPasswordSet());
            assertEquals("encodedNewPassword", oauthUser.getPasswordHash());
            verify(cacheService).evict(CacheService.vKey("user:profile:10"));
        }

        @Test
        @DisplayName("OAuth - Setup password rejected for user who already has password")
        void setupPassword_shouldRejectIfPasswordAlreadySet() {
            when(authUserRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));

            SetupPasswordRequest request = new SetupPasswordRequest("verified@example.com", "anotherPass");

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> authService.setupPassword(request));
            assertTrue(ex.getMessage().contains("already set"));
        }
    }
}
