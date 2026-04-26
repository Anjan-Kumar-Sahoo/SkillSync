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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpTokenRepository otpTokenRepository;

    @Mock
    private AuthUserRepository authUserRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private OtpService otpService;

    private AuthUser testUser;
    private OtpToken testOtpToken;

    @BeforeEach
    void setUp() {
        testUser = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .isVerified(false)
                .build();

        testOtpToken = OtpToken.builder()
                .id(1L)
                .userId(1L)
                .email("test@example.com")
                .otp("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .used(false)
                .attempts(0)
                .type(OtpType.REGISTRATION)
                .build();
    }

    @Test
    @DisplayName("Generate and Send OTP - Success")
    void generateAndSendOtp_shouldSaveAndSend() {
        otpService.generateAndSendOtp(testUser, OtpType.REGISTRATION);

        verify(otpTokenRepository, times(1)).save(any(OtpToken.class));
        verify(emailService, times(1)).sendOtpEmail(eq("test@example.com"), anyString(), eq("John"), eq(OtpType.REGISTRATION));
    }

    @Test
    @DisplayName("Resend OTP - Success")
    void resendOtp_shouldWork() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        otpService.resendOtp("test@example.com");

        verify(otpTokenRepository, times(1)).save(any(OtpToken.class));
        verify(emailService, times(1)).sendOtpEmail(anyString(), anyString(), anyString(), any());
    }

    @Test
    @DisplayName("Resend OTP - Fails if already verified")
    void resendOtp_alreadyVerified_shouldThrow() {
        testUser.setVerified(true);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(RuntimeException.class, () -> otpService.resendOtp("test@example.com"));
    }

    @Test
    @DisplayName("Verify OTP - Success")
    void verifyOtp_success_shouldReturnTrue() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.of(testOtpToken));

        boolean result = otpService.verifyOtp("test@example.com", "123456", OtpType.REGISTRATION);

        assertTrue(result);
        assertTrue(testOtpToken.isUsed());
        assertTrue(testUser.isVerified());
        verify(otpTokenRepository).save(testOtpToken);
        verify(authUserRepository).save(testUser);
    }

    @Test
    @DisplayName("Verify OTP - Incorrect OTP")
    void verifyOtp_incorrect_shouldIncrementAttempts() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.of(testOtpToken));

        assertThrows(RuntimeException.class, () -> otpService.verifyOtp("test@example.com", "wrong", OtpType.REGISTRATION));

        assertEquals(1, testOtpToken.getAttempts());
        verify(otpTokenRepository).save(testOtpToken);
    }

    @Test
    @DisplayName("Verify OTP - Max attempts reached")
    void verifyOtp_maxAttempts_shouldRollbackAndThrow() {
        testOtpToken.setAttempts(5);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.of(testOtpToken));

        assertThrows(RuntimeException.class, () -> otpService.verifyOtp("test@example.com", "123456", OtpType.REGISTRATION));

        assertTrue(testOtpToken.isUsed());
        verify(otpTokenRepository).deleteByEmail("test@example.com");
        verify(authUserRepository).deleteByEmail("test@example.com");
    }

    @Test
    @DisplayName("Verify OTP - Expired or not found")
    void verifyOtp_expired_shouldRollbackAndThrow() {
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(otpTokenRepository.findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(anyString(), any(), any()))
                .thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> otpService.verifyOtp("test@example.com", "123456", OtpType.REGISTRATION));

        verify(otpTokenRepository).deleteByEmail("test@example.com");
        verify(authUserRepository).deleteByEmail("test@example.com");
    }

    @Test
    @DisplayName("Cleanup Expired OTPs")
    void cleanupExpiredOtps_shouldCallRepository() {
        otpService.cleanupExpiredOtps();
        verify(otpTokenRepository).deleteByExpiresAtBefore(any());
    }
}
