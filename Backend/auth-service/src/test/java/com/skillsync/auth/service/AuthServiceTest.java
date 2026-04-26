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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthUserRepository authUserRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private OtpService otpService;
    @Mock
    private EmailService emailService;
    @Mock
    private CacheService cacheService;

    @InjectMocks
    private AuthService authService;

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
    @DisplayName("Refresh token - expired token is deleted and rejected")
    void refreshToken_expired_shouldDeleteAndThrow() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token")
                .user(verifiedUser)
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .build();

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.refreshToken(new RefreshTokenRequest("expired-token")));

        assertTrue(ex.getMessage().contains("expired"));
        verify(refreshTokenRepository).delete(expired);
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

    @Test
    @DisplayName("Update user name - blank values are rejected")
    void updateUserName_blankValues_shouldThrow() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> authService.updateUserName(1L, " ", "Doe"));

        assertTrue(ex.getMessage().contains("required"));
        verify(authUserRepository, never()).save(any(AuthUser.class));
        verify(cacheService, never()).evict(anyString());
    }

    @Test
    @DisplayName("Reset password - cannot reuse current password")
    void resetPassword_sameAsCurrent_shouldThrow() {
        ResetPasswordRequest request = new ResetPasswordRequest(
                "verified@example.com",
                "123456",
                "ValidPassword123!");

        when(authUserRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));
        when(passwordEncoder.matches("ValidPassword123!", verifiedUser.getPasswordHash())).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.resetPassword(request));

        assertTrue(ex.getMessage().contains("different from current password"));
        verify(otpService).verifyOtp("verified@example.com", "123456", OtpType.PASSWORD_RESET);
        verify(refreshTokenRepository, never()).deleteByUser(any(AuthUser.class));
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
        @DisplayName("OAuth - Unverified user → auto-verify and allow login")
        void oauth_unverifiedUser_shouldBeVerifiedAndLoggedIn() {
            OAuthRequest request = new OAuthRequest(
                    "unverified@example.com", "Unverified", "User", "google", "google-id-789");

            AuthUser unverifiedUser = AuthUser.builder()
                    .id(3L).email("unverified@example.com").firstName("Unverified").lastName("User")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(false).passwordSet(true).build();

            when(authUserRepository.findByProviderAndProviderId("google", "google-id-789"))
                    .thenReturn(Optional.empty());
            when(authUserRepository.findByEmail("unverified@example.com"))
                    .thenReturn(Optional.of(unverifiedUser));

            // Mock token generation for successful login
            when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("accessToken");
            when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refreshToken");
            when(jwtTokenProvider.getAccessExpiration()).thenReturn(3600000L);
            when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

            OAuthResponse response = authService.loginWithOAuth(request);

            assertNotNull(response);
            assertTrue(unverifiedUser.isVerified(), "OAuth login should auto-verify the user");
            assertEquals("accessToken", response.accessToken());
            // Should NOT create new user but MIGHT save existing user if provider was null
            verify(authUserRepository).save(unverifiedUser);
        }

        @Test
        @DisplayName("OAuth - Setup password for new OAuth user")
        void setupPassword_shouldSetPasswordForOAuthUser() {
            AuthUser oauthUser = AuthUser.builder()
                    .id(10L).email("oauth@gmail.com").firstName("OAuth").lastName("User")
                    .role(Role.ROLE_LEARNER).isActive(true).isVerified(true).passwordSet(false)
                    .provider("google").providerId("google-id-123").build();

            when(authUserRepository.findByEmail("oauth@gmail.com")).thenReturn(Optional.of(oauthUser));
            when(passwordEncoder.encode("MyNewPassword1!")).thenReturn("encodedNewPassword");
            when(authUserRepository.save(any())).thenReturn(oauthUser);

            SetupPasswordRequest request = new SetupPasswordRequest("oauth@gmail.com", "MyNewPassword1!");
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

    @Nested
    @DisplayName("Registration Flow Improvements")
    class RegistrationFlowTests {

        @Test
        @DisplayName("Initiate Registration - New User")
        void initiateRegistration_newUser_shouldCreateAndSendOtp() {
            InitiateRegistrationRequest request = new InitiateRegistrationRequest("new@example.com");
            when(authUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            when(authUserRepository.save(any())).thenReturn(testUser);

            Map<String, Object> response = authService.initiateRegistration(request);

            assertFalse((Boolean) response.get("exists"));
            assertTrue((Boolean) response.get("otpSent"));
            verify(otpService).generateAndSendOtp(any(), eq(OtpType.REGISTRATION));
        }

        @Test
        @DisplayName("Initiate Registration - Unverified Existing User")
        void initiateRegistration_unverifiedExisting_shouldResendOtp() {
            InitiateRegistrationRequest request = new InitiateRegistrationRequest("test@example.com");
            when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            Map<String, Object> response = authService.initiateRegistration(request);

            assertFalse((Boolean) response.get("exists"));
            assertTrue((Boolean) response.get("otpSent"));
            verify(otpService).generateAndSendOtp(eq(testUser), eq(OtpType.REGISTRATION));
        }

        @Test
        @DisplayName("Complete Registration - Success")
        void completeRegistration_success_shouldUpdateUser() {
            testUser.setVerified(true);
            testUser.setPasswordSet(false);
            testUser.setPasswordHash("PENDING");
            CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                    "test@example.com", "Password123!", "John", "Doe");

            when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPassword");
            when(authUserRepository.save(any())).thenReturn(testUser);

            AuthResponse response = authService.completeRegistration(request);

            assertNotNull(response);
            assertTrue(testUser.isPasswordSet());
            assertEquals("encodedNewPassword", testUser.getPasswordHash());
            verify(emailService).sendWelcomeEmail(anyString(), anyString());
        }

        @Test
        @DisplayName("Complete Registration - Fails if not verified")
        void completeRegistration_unverified_shouldThrow() {
            CompleteRegistrationRequest request = new CompleteRegistrationRequest(
                    "test@example.com", "Pass123!", "J", "D");
            when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

            assertThrows(RuntimeException.class, () -> authService.completeRegistration(request));
        }
    }

    @Nested
    @DisplayName("Password Management Tests")
    class PasswordManagementTests {
        @Test
        @DisplayName("Forgot Password - Success")
        void forgotPassword_shouldSendOtp() {
            when(authUserRepository.findByEmail("verified@example.com")).thenReturn(Optional.of(verifiedUser));
            authService.forgotPassword(new ForgotPasswordRequest("verified@example.com"));
            verify(otpService).generateAndSendOtp(verifiedUser, OtpType.PASSWORD_RESET);
        }

        @Test
        @DisplayName("Verify Password Reset OTP - Success")
        void verifyPasswordResetOtp_shouldCallOtpService() {
            authService.verifyPasswordResetOtp("test@example.com", "123456");
            verify(otpService).validateOtp("test@example.com", "123456", OtpType.PASSWORD_RESET);
        }
    }

    @Nested
    @DisplayName("User Management Tests")
    class UserManagementTests {
        @Test
        @DisplayName("Get User By ID - Success")
        void getUserById_shouldReturnSummary() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            UserSummary summary = authService.getUserById(1L);
            assertEquals("test@example.com", summary.email());
        }

        @Test
        @DisplayName("Delete User - Success")
        void deleteUser_shouldInvokeRepository() {
            when(authUserRepository.findById(1L)).thenReturn(Optional.of(testUser));
            authService.deleteUser(1L);
            verify(refreshTokenRepository).deleteByUser(testUser);
            verify(authUserRepository).delete(testUser);
        }
    }
}
