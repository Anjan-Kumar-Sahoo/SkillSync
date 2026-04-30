package com.skillsync.auth.service;

import com.skillsync.auth.dto.*;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.RefreshToken;
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
}
