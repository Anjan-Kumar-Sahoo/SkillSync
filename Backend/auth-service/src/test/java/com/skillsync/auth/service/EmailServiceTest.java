package com.skillsync.auth.service;

import com.skillsync.auth.enums.OtpType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "test@skillsync.com");
        ReflectionTestUtils.setField(emailService, "appBaseUrl", "http://localhost:3000");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Send OTP Email - Registration Type")
    void sendOtpEmail_registration_shouldSendEmail() {
        emailService.sendOtpEmail("user@example.com", "123456", "John", OtpType.REGISTRATION);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send OTP Email - Password Reset Type")
    void sendOtpEmail_passwordReset_shouldSendEmail() {
        emailService.sendOtpEmail("user@example.com", "123456", "John", OtpType.PASSWORD_RESET);

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send Welcome Email")
    void sendWelcomeEmail_shouldSendEmail() {
        emailService.sendWelcomeEmail("user@example.com", "John");

        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send Email - Handle MessagingException")
    void sendHtmlEmail_shouldHandleMessagingException() throws MessagingException {
        // Since sendHtmlEmail is private, we call a public method that triggers it
        // We need to mock the send method to throw an exception
        doThrow(new RuntimeException("Mail server down")).when(mailSender).send(any(MimeMessage.class));

        // This should not throw an exception as it's caught in sendHtmlEmail
        emailService.sendWelcomeEmail("user@example.com", "John");

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Send OTP Email - Handle null firstName")
    void sendOtpEmail_nullFirstName_shouldUseDefault() {
        emailService.sendOtpEmail("user@example.com", "123456", null, OtpType.REGISTRATION);

        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}
