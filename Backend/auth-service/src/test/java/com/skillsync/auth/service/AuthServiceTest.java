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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private OtpService otpService;
    @Mock private EmailService emailService;
    @Mock private CacheService cacheService;
    @InjectMocks private AuthService authService;

    private AuthUser user;

    @BeforeEach
    void setUp() {
        user = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_LEARNER)
                .isVerified(true)
                .build();
    }

    @Test @DisplayName("register - Success")
    void registerSuccess() {
        RegisterRequest request = new RegisterRequest("test@example.com", "Password@123", "John", "Doe");
        when(authUserRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(user);

        AuthResponse response = authService.register(request);

        assertThat(response.user().email()).isEqualTo("test@example.com");
        verify(otpService).generateAndSendOtp(any(), any());
    }

    @Test @DisplayName("login - Success")
    void loginSuccess() {
        LoginRequest request = new LoginRequest("test@example.com", "Password@123");
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
        when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(Collections.emptyList());

        AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("access");
        verify(authenticationManager).authenticate(any());
    }

    @Test @DisplayName("login - Unverified")
    void loginUnverified() {
        user.setVerified(false);
        LoginRequest request = new LoginRequest("test@example.com", "Password@123");
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not verified");
    }

    @Test @DisplayName("refreshToken - Success")
    void refreshTokenSuccess() {
        RefreshToken token = RefreshToken.builder().token("old-token").user(user).expiresAt(java.time.LocalDateTime.now().plusDays(1)).build();
        when(refreshTokenRepository.findByToken("old-token")).thenReturn(Optional.of(token));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("new-access");

        AuthResponse response = authService.refreshToken(new RefreshTokenRequest("old-token"));

        assertThat(response.accessToken()).isEqualTo("new-access");
        verify(refreshTokenRepository).delete(token);
    }

    @Test @DisplayName("updateUserRole - Success")
    void updateUserRole() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        authService.updateUserRole(1L, "ROLE_MENTOR");
        assertThat(user.getRole()).isEqualTo(Role.ROLE_MENTOR);
        verify(cacheService).evict(any());
    }

    // --- PHASE 1: REGISTRATION FLOW ---

    @Test @DisplayName("initiateRegistration - New User")
    void initiateRegistration_NewUser() {
        InitiateRegistrationRequest request = new InitiateRegistrationRequest("new@example.com");
        when(authUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(user);

        var result = authService.initiateRegistration(request);

        assertThat(result.get("otpSent")).isEqualTo(true);
        verify(otpService).generateAndSendOtp(any(), eq(OtpType.REGISTRATION));
    }

    @Test @DisplayName("initiateRegistration - Existing Verified")
    void initiateRegistration_ExistingVerified() {
        InitiateRegistrationRequest request = new InitiateRegistrationRequest("test@example.com");
        user.setVerified(true);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var result = authService.initiateRegistration(request);

        assertThat(result.get("exists")).isEqualTo(true);
        verify(otpService, never()).generateAndSendOtp(any(), any());
    }

    @Test @DisplayName("initiateRegistration - Existing Unverified")
    void initiateRegistration_ExistingUnverified() {
        InitiateRegistrationRequest request = new InitiateRegistrationRequest("test@example.com");
        user.setVerified(false);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        var result = authService.initiateRegistration(request);

        assertThat(result.get("otpSent")).isEqualTo(true);
        verify(otpService).generateAndSendOtp(user, OtpType.REGISTRATION);
    }

    @Test @DisplayName("completeRegistration - Success")
    void completeRegistration_Success() {
        CompleteRegistrationRequest request = new CompleteRegistrationRequest("test@example.com", "Strong@123", "John", "Doe");
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setPasswordHash("PENDING");
        
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("Strong@123")).thenReturn("hashed_pass");
        when(authUserRepository.save(any())).thenReturn(user);

        AuthResponse response = authService.completeRegistration(request);

        assertThat(response.user().firstName()).isEqualTo("John");
        assertThat(user.isPasswordSet()).isTrue();
        verify(emailService).sendWelcomeEmail(eq("test@example.com"), anyString());
    }

    @Test @DisplayName("completeRegistration - Unverified Error")
    void completeRegistration_Unverified() {
        CompleteRegistrationRequest request = new CompleteRegistrationRequest("test@example.com", "Strong@123", "John", "Doe");
        user.setVerified(false);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.completeRegistration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test @DisplayName("completeRegistration - Already Registered Error")
    void completeRegistration_AlreadyRegistered() {
        CompleteRegistrationRequest request = new CompleteRegistrationRequest("test@example.com", "Strong@123", "John", "Doe");
        user.setVerified(true);
        user.setPasswordSet(true);
        user.setPasswordHash("existing_hash");
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.completeRegistration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Registration already completed");
    }

    @Test @DisplayName("completeRegistration - Email Service Exception (Should not fail registration)")
    void completeRegistration_EmailFailure() {
        CompleteRegistrationRequest request = new CompleteRegistrationRequest("test@example.com", "Strong@123", "John", "Doe");
        user.setVerified(true);
        user.setPasswordSet(false);
        user.setPasswordHash("PENDING");

        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(authUserRepository.save(any())).thenReturn(user);
        doThrow(new RuntimeException("Email down")).when(emailService).sendWelcomeEmail(any(), any());

        AuthResponse response = authService.completeRegistration(request);

        assertThat(response).isNotNull();
        verify(authUserRepository).save(user);
    }

    // --- PHASE 2: PASSWORD RECOVERY ---

    @Test @DisplayName("forgotPassword - Success")
    void forgotPassword_Success() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("test@example.com");
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        authService.forgotPassword(request);

        verify(otpService).generateAndSendOtp(user, OtpType.PASSWORD_RESET);
    }

    @Test @DisplayName("forgotPassword - User Not Found")
    void forgotPassword_NotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest("unknown@example.com");
        when(authUserRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword(request))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("verifyPasswordResetOtp - Success")
    void verifyPasswordResetOtp_Success() {
        authService.verifyPasswordResetOtp("test@example.com", "123456");
        verify(otpService).validateOtp("test@example.com", "123456", OtpType.PASSWORD_RESET);
    }

    @Test @DisplayName("resetPassword - Success")
    void resetPassword_Success() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "123456", "New@Pass123");
        user.setPasswordHash("old_hashed_pass");
        
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("New@Pass123", "old_hashed_pass")).thenReturn(false);
        when(passwordEncoder.encode("New@Pass123")).thenReturn("new_hashed_pass");

        authService.resetPassword(request);

        verify(otpService).verifyOtp("test@example.com", "123456", OtpType.PASSWORD_RESET);
        assertThat(user.getPasswordHash()).isEqualTo("new_hashed_pass");
        verify(refreshTokenRepository).deleteByUser(user);
    }

    @Test @DisplayName("resetPassword - Same Password Error")
    void resetPassword_SamePasswordError() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "123456", "New@Pass123");
        user.setPasswordHash("hashed_pass");
        
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("New@Pass123", "hashed_pass")).thenReturn(true);

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("must be different");
    }

    @Test @DisplayName("resetPassword - Weak Password Error")
    void resetPassword_WeakPassword() {
        ResetPasswordRequest request = new ResetPasswordRequest("test@example.com", "123456", "weak");
        
        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password must be 8-100 chars");
    }

    // --- PHASE 3: OAUTH & PASSWORD SETUP ---

    @Test @DisplayName("loginWithOAuth - New User")
    void loginWithOAuth_NewUser() {
        OAuthRequest request = new OAuthRequest("oauth@example.com", "John", "Doe", "google", "g-123");
        when(authUserRepository.findByProviderAndProviderId("google", "g-123")).thenReturn(Optional.empty());
        when(authUserRepository.findByEmail("oauth@example.com")).thenReturn(Optional.empty());
        when(authUserRepository.save(any())).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");

        OAuthResponse response = authService.loginWithOAuth(request);

        assertThat(response.passwordSetupRequired()).isTrue();
        verify(authUserRepository).save(any(AuthUser.class));
    }

    @Test @DisplayName("loginWithOAuth - Existing User Link Provider")
    void loginWithOAuth_LinkProvider() {
        OAuthRequest request = new OAuthRequest("test@example.com", "John", "Doe", "google", "g-123");
        user.setProvider(null);
        user.setVerified(false);
        
        when(authUserRepository.findByProviderAndProviderId("google", "g-123")).thenReturn(Optional.empty());
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authUserRepository.save(any())).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access");

        authService.loginWithOAuth(request);

        assertThat(user.getProvider()).isEqualTo("google");
        assertThat(user.isVerified()).isTrue();
        verify(cacheService).evict(any());
    }

    @Test @DisplayName("setupPassword - Success")
    void setupPassword_Success() {
        SetupPasswordRequest request = new SetupPasswordRequest("test@example.com", "New@Pass123");
        user.setPasswordSet(false);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("New@Pass123")).thenReturn("hashed");

        authService.setupPassword(request);

        assertThat(user.isPasswordSet()).isTrue();
        verify(cacheService).evict(any());
    }

    @Test @DisplayName("setupPassword - Already Set Error")
    void setupPassword_AlreadySet() {
        SetupPasswordRequest request = new SetupPasswordRequest("test@example.com", "New@Pass123");
        user.setPasswordSet(true);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.setupPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already set");
    }

    // --- USER MANAGEMENT ---

    @Test @DisplayName("logout - Success")
    void logout_Success() {
        RefreshToken token = RefreshToken.builder().token("token").build();
        when(refreshTokenRepository.findByToken("token")).thenReturn(Optional.of(token));
        
        authService.logout("token");
        
        verify(refreshTokenRepository).delete(token);
    }

    @Test @DisplayName("updateUserName - Success")
    void updateUserName_Success() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        authService.updateUserName(1L, " Jane ", " Smith ");
        
        assertThat(user.getFirstName()).isEqualTo("Jane");
        assertThat(user.getLastName()).isEqualTo("Smith");
        verify(cacheService).evict(any());
    }

    @Test @DisplayName("updateUserName - Blank Name Error")
    void updateUserName_Blank() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> authService.updateUserName(1L, "", "Doe"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("deleteUser - Success")
    void deleteUser_Success() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        authService.deleteUser(1L);
        verify(refreshTokenRepository).deleteByUser(user);
        verify(authUserRepository).delete(user);
    }
    @Test @DisplayName("getUserCount - Success")
    void getUserCount() {
        when(authUserRepository.countByRole(Role.ROLE_MENTOR)).thenReturn(10L);
        assertThat(authService.getUserCount("ROLE_MENTOR")).isEqualTo(10L);
    }

    @Test @DisplayName("getUserCount - All Users")
    void getUserCount_All() {
        when(authUserRepository.count()).thenReturn(100L);
        assertThat(authService.getUserCount(null)).isEqualTo(100L);
    }

    @Test @DisplayName("getUserById - Success")
    void getUserById_Success() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        var summary = authService.getUserById(1L);
        assertThat(summary.email()).isEqualTo("test@example.com");
    }

    @Test @DisplayName("getUserById - Not Found")
    void getUserById_NotFound() {
        when(authUserRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authService.getUserById(99L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("tokenEviction - Success")
    void tokenEviction() {
        LoginRequest request = new LoginRequest("test@example.com", "Strong@123");
        List<RefreshToken> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < 5; i++) tokens.add(RefreshToken.builder().build());
        
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(any())).thenReturn(tokens);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("a");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("r");

        authService.login(request);

        verify(refreshTokenRepository).delete(any()); // Should delete the oldest token
    }

    @Test @DisplayName("loginWithOAuth - Link Existing Verified User")
    void loginWithOAuth_LinkVerified() {
        OAuthRequest request = new OAuthRequest("test@example.com", "John", "Doe", "google", "g-123");
        user.setProvider(null);
        user.setVerified(true);
        
        when(authUserRepository.findByProviderAndProviderId("google", "g-123")).thenReturn(Optional.empty());
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(authUserRepository.save(any())).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access");

        authService.loginWithOAuth(request);

        assertThat(user.getProvider()).isEqualTo("google");
    }

    @Test @DisplayName("getAllUsers - Success")
    void getAllUsers() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id"));
        var usersPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));
        
        when(authUserRepository.findByFilters(null, null, pageable)).thenReturn(usersPage);
        
        var result = authService.getAllUsers(0, 10);
        assertThat(result.get("totalElements")).isEqualTo(1L);
    }

    @Test @DisplayName("getAllUsersFiltered - With Role and Search")
    void getAllUsersFiltered() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("id"));
        var usersPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(user));
        
        when(authUserRepository.findByFilters(eq(Role.ROLE_LEARNER), eq("test"), any())).thenReturn(usersPage);
        
        var result = authService.getAllUsersFiltered(0, 10, "ROLE_LEARNER", "test");
        assertThat(result.get("totalElements")).isEqualTo(1L);
    }
}
