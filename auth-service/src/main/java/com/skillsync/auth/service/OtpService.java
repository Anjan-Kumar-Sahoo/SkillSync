package com.skillsync.auth.service;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.OtpToken;
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
    public void generateAndSendOtp(AuthUser user) {
        String otp = generateOtp();

        OtpToken otpToken = OtpToken.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .otp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .used(false)
                .attempts(0)
                .build();

        otpTokenRepository.save(otpToken);
        emailService.sendOtpEmail(user.getEmail(), otp, user.getFirstName());

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

        generateAndSendOtp(user);
        log.info("OTP resent to: {}", email);
    }

    /**
     * Verify the OTP submitted by the user.
     * Marks the user as verified on success.
     */
    @Transactional
    public boolean verifyOtp(String email, String otp) {
        AuthUser user = authUserRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

        if (user.isVerified()) {
            throw new RuntimeException("Email is already verified");
        }

        OtpToken otpToken = otpTokenRepository
                .findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(email, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("No valid OTP found. Please request a new one."));

        // Check max attempts
        if (otpToken.getAttempts() >= MAX_ATTEMPTS) {
            otpToken.setUsed(true); // Invalidate it
            otpTokenRepository.save(otpToken);
            throw new RuntimeException("Too many failed attempts. Please request a new OTP.");
        }

        if (!otpToken.getOtp().equals(otp)) {
            otpToken.setAttempts(otpToken.getAttempts() + 1);
            otpTokenRepository.save(otpToken);
            throw new RuntimeException("Invalid OTP. Attempts remaining: " + (MAX_ATTEMPTS - otpToken.getAttempts()));
        }

        // OTP is valid — mark as used and verify the user
        otpToken.setUsed(true);
        otpTokenRepository.save(otpToken);

        user.setVerified(true);
        authUserRepository.save(user);

        log.info("Email verified for user: {}", email);
        return true;
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
