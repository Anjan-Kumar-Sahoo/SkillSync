package com.skillsync.auth.service;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.OtpToken;
import com.skillsync.auth.enums.OtpType;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final AuthUserRepository authUserRepository;
    private final EmailService emailService;

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates and sends a 6-digit OTP to the user's email after registration.
     */
    @Transactional
    public void generateAndSendOtp(AuthUser user, OtpType type) {
        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .attempts(0)
                .type(type)
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(user.getEmail(), otp, user.getFirstName(), type);

        log.info("OTP generated and sent to: {}", user.getEmail());
    }

    /**
     * Resend OTP — generates a fresh OTP for the given email.
     */
    @Transactional
    public void resendOtp(String email) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.isVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        generateAndSendOtp(user, OtpType.REGISTRATION);
        log.info("OTP resent to: {}", email);
    }

    /**
     * Verify the OTP submitted by the user.
     * Marks the user as verified on success.
     */
    @Transactional
    public boolean verifyOtp(String email, String otp, OtpType type) {
        return verifyOtp(email, otp, type, true);
    }

    /**
     * Validate OTP without consuming it.
     * Used by password reset flow before final password submission.
     */
    @Transactional
    public boolean validateOtp(String email, String otp, OtpType type) {
        return verifyOtp(email, otp, type, false);
    }

    private boolean verifyOtp(String email, String otp, OtpType type, boolean consumeToken) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (type == OtpType.REGISTRATION && user.isVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndTypeAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, type, LocalDateTime.now())
                .orElse(null);

        // Fail Case 1: Expired or not found
        if (otpToken == null) {
            if (type == OtpType.REGISTRATION && !user.isVerified()) {
                rollbackRegistration(email);
                throw new RuntimeException("OTP expired or not found. Registration rolled back for security. Please register again.");
            }
            throw new RuntimeException("No valid OTP found. Please request a new one.");
        }

        // Fail Case 2: Max attempts
        if (otpToken.getAttempts() >= MAX_ATTEMPTS) {
            otpToken.setUsed(true);
            otpTokenRepository.save(otpToken);
            if (type == OtpType.REGISTRATION) {
                rollbackRegistration(email);
                throw new RuntimeException("Too many failed attempts. Registration rolled back. Please register again.");
            }
            throw new RuntimeException("Too many failed attempts. Please request a new OTP.");
        }

        // Fail Case 3: Incorrect OTP
        if (!otpToken.getOtp().equals(otp)) {
            otpToken.setAttempts(otpToken.getAttempts() + 1);
            otpTokenRepository.save(otpToken);
            throw new RuntimeException("Invalid OTP. Attempts remaining: " + (MAX_ATTEMPTS - otpToken.getAttempts()));
        }

        // Success
        if (consumeToken) {
            otpToken.setUsed(true);
            otpTokenRepository.save(otpToken);
        }

        if (type == OtpType.REGISTRATION) {
            user.setVerified(true);
            authUserRepository.save(user);
        }

        log.info("OTP {} for: {} type: {}", consumeToken ? "verified" : "validated", email, type);
        return true;
    }

    private void rollbackRegistration(String email) {
        log.warn("Rolling back registration for email: {}", email);
        otpTokenRepository.deleteByEmail(email);
        authUserRepository.deleteByEmail(email);
    }

    private String generateOtp() {
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        int number = secureRandom.nextInt(max - min + 1) + min;
        return String.valueOf(number);
    }

    /**
     * Scheduled cleanup: remove expired OTP tokens every hour.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void cleanupExpiredOtps() {
        otpTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        log.info("Expired OTP tokens cleaned up");
    }
}
