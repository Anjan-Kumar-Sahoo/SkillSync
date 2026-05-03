package com.skillsync.auth.service;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.OtpToken;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.OtpTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceCoverageGapTest {

    @Mock private OtpTokenRepository otpTokenRepository;
    @Mock private AuthUserRepository authUserRepository;
    @InjectMocks private OtpService otpService;

    private AuthUser user;
    private OtpToken token;

    @BeforeEach
    void setUp() {
        user = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .isVerified(true)
                .build();

        token = OtpToken.builder()
                .id(1L)
                .userId(1L)
                .email("test@example.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(0)
                .type(OtpType.PASSWORD_RESET)
                .build();
    }

    @Test
    @DisplayName("Verify OTP - PASSWORD_RESET Expired")
    void verifyOtp_PasswordResetExpired() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp("test@example.com", "123456", OtpType.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No valid OTP found");

        // Should not rollback registration for PASSWORD_RESET
        verify(otpTokenRepository, never()).deleteByEmail(anyString());
        verify(authUserRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("Verify OTP - PASSWORD_RESET Max Attempts")
    void verifyOtp_PasswordResetMaxAttempts() {
        token.setAttempts(5);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp("test@example.com", "123456", OtpType.PASSWORD_RESET))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Too many failed attempts");

        // Should save token as used but not rollback
        verify(otpTokenRepository).save(token);
        assertThat(token.isUsed()).isTrue();
        verify(authUserRepository, never()).deleteByEmail(anyString());
    }

    @Test
    @DisplayName("Validate OTP - PASSWORD_RESET Success (consumeToken = false)")
    void validateOtp_Success() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.of(token));

        boolean result = otpService.validateOtp("test@example.com", "123456", OtpType.PASSWORD_RESET);

        assertThat(result).isTrue();
        assertThat(token.isUsed()).isFalse(); // Should not consume
        verify(otpTokenRepository, never()).save(any());
    }
}
