package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.EmailRetryEvent;
import com.skillsync.notification.service.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailRetryConsumerTest {

    @Mock private EmailService emailService;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private EmailRetryConsumer emailRetryConsumer;

    @Test
    @DisplayName("Email retry - successful on retry attempt")
    void handleEmailRetry_shouldSucceedOnRetry() throws Exception {
        EmailRetryEvent event = new EmailRetryEvent(
                "user@test.com", "Subject", "session-booked",
                Map.of("recipientName", "John"), 0, "Connection timeout");

        // Simulate successful send on retry
        doNothing().when(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());

        emailRetryConsumer.handleEmailRetry(event);

        verify(emailService).doSendEmail("user@test.com", "Subject", "session-booked",
                Map.of("recipientName", "John"));
        // Should NOT re-publish since it succeeded
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(EmailRetryEvent.class));
    }

    @Test
    @DisplayName("Email retry - failure triggers re-queue with incremented count")
    void handleEmailRetry_shouldRequeueOnFailure() throws Exception {
        EmailRetryEvent event = new EmailRetryEvent(
                "user@test.com", "Subject", "session-booked",
                Map.of("recipientName", "John"), 0, "Connection timeout");

        doThrow(new MessagingException("SMTP error"))
                .when(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());

        emailRetryConsumer.handleEmailRetry(event);

        // Should re-publish with incremented retry count
        ArgumentCaptor<EmailRetryEvent> captor = ArgumentCaptor.forClass(EmailRetryEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_RETRY_EXCHANGE), eq("email.retry"), captor.capture());

        EmailRetryEvent retried = captor.getValue();
        assertEquals(1, retried.retryCount(), "Retry count should increment");
        assertEquals("user@test.com", retried.to());
    }

    @Test
    @DisplayName("Email retry - max retries reached, no more re-queue")
    void handleEmailRetry_shouldStopAfterMaxRetries() throws Exception {
        // Already at retry count 2 (3rd attempt will be the max)
        EmailRetryEvent event = new EmailRetryEvent(
                "user@test.com", "Subject", "session-booked",
                Map.of("recipientName", "John"), 2, "Persistent SMTP error");

        doThrow(new MessagingException("SMTP error"))
                .when(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());

        emailRetryConsumer.handleEmailRetry(event);

        // Should NOT re-publish — max retries exhausted
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(EmailRetryEvent.class));
    }
}
