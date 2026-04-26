package com.skillsync.notification.service;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.EmailRetryEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@skillsync.local");
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Send Email - Success")
    void sendEmail_success_shouldSend() throws MessagingException {
        Map<String, Object> variables = new HashMap<>();
        variables.put("name", "John");
        
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");

        emailService.sendEmail("test@example.com", "Subject", "test-template", variables);

        verify(mailSender).send(any(MimeMessage.class));
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(EmailRetryEvent.class));
    }

    @Test
    @DisplayName("Send Email - Failure triggers Retry")
    void sendEmail_failure_shouldRetry() throws MessagingException {
        Map<String, Object> variables = new HashMap<>();
        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test</html>");
        doThrow(new RuntimeException("SMTP Error")).when(mailSender).send(any(MimeMessage.class));

        emailService.sendEmail("test@example.com", "Subject", "test-template", variables);

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EMAIL_RETRY_EXCHANGE), eq("email.retry"), any(EmailRetryEvent.class));
    }

    @Test
    @DisplayName("Build Details HTML - Success with Escaping")
    void buildDetailsHtml_shouldReturnTable() {
        Map<String, String> details = new HashMap<>();
        details.put("Key &", "Value <");

        String html = emailService.buildDetailsHtml(details);

        assertTrue(html.contains("<table"));
        assertTrue(html.contains("Key &amp;"));
        assertTrue(html.contains("Value &lt;"));
    }

    @Test
    @DisplayName("Build Details HTML - Empty Map")
    void buildDetailsHtml_empty_shouldReturnEmptyString() {
        assertEquals("", emailService.buildDetailsHtml(new HashMap<>()));
        assertEquals("", emailService.buildDetailsHtml(null));
    }
}
