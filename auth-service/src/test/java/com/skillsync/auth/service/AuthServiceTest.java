package com.skillsync.auth.service;

import com.skillsync.auth.dto.AuthResponse;
import com.skillsync.auth.dto.LoginRequest;
import com.skillsync.auth.dto.RegisterRequest;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.RefreshTokenRepository;
import com.skillsync.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

        // Verify OTP was actually triggered with the correct user
        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(otpService, times(1)).generateAndSendOtp(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
    }

    @Test
    @DisplayName("Register - duplicate email throws exception")
    void register_shouldThrowExceptionForDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        when(authUserRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThrows(RuntimeException.class, () -> authService.register(request));
        verify(authUserRepository, never()).save(any());
        verify(otpService, never()).generateAndSendOtp(any());
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
        // OTP should NOT be sent for already-verified users
        verify(otpService, never()).generateAndSendOtp(any());
    }

    @Test
    @DisplayName("Login - unverified user is blocked and OTP is re-sent")
    void login_shouldRejectUnverifiedUserAndResendOtp() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");

        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> authService.login(request));

        assertTrue(ex.getMessage().contains("Email not verified"));
        assertTrue(ex.getMessage().contains("A new OTP has been sent"));

        // Verify OTP was re-sent to the unverified user
        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(otpService, times(1)).generateAndSendOtp(userCaptor.capture());
        assertEquals("test@example.com", userCaptor.getValue().getEmail());
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
}
