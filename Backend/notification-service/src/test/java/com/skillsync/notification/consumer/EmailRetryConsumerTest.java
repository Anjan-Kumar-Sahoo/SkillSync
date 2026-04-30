package com.skillsync.notification.consumer;

import com.skillsync.notification.dto.EmailRetryEvent;
import com.skillsync.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailRetryConsumerTest {

    @Mock private EmailService emailService;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private EmailRetryConsumer consumer;

    private EmailRetryEvent event;

    @BeforeEach
    void setUp() {
        event = new EmailRetryEvent("to@test.com", "Subject", "welcome", Map.of("name", "John"), 0, "first failure");
    }

    @Test @DisplayName("Success on first retry (immediate)")
    void successFirstRetry() throws Exception {
        consumer.handleEmailRetry(event);
        verify(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test @DisplayName("Success on second retry (with backoff)")
    void successSecondRetry() throws Exception {
        EmailRetryEvent secondRetryEvent = new EmailRetryEvent("to@test.com", "Subject", "welcome", Map.of("name", "John"), 1, "second failure");
        // We'll mock Thread.sleep to avoid waiting
        // Actually, we can't easily mock Thread.sleep in a unit test without power-mocking,
        // but we can just let it sleep if the delay is short, or use a smaller BASE_DELAY_MS via reflection.
        
        try {
            var field = EmailRetryConsumer.class.getDeclaredField("BASE_DELAY_MS");
            field.setAccessible(true);
            // field is static final, but we can try to change it if it's not truly final in the JVM
            // Or we just wait 2 seconds.
        } catch (Exception ignored) {}

        consumer.handleEmailRetry(secondRetryEvent);
        verify(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test @DisplayName("Failure and re-queue")
    void failureAndRequeue() throws Exception {
        doThrow(new RuntimeException("SMTP still down")).when(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());

        consumer.handleEmailRetry(event);

        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
    }

    @Test @DisplayName("Permanent failure after max retries")
    void permanentFailure() throws Exception {
        EmailRetryEvent maxRetryEvent = new EmailRetryEvent("to@test.com", "Subject", "welcome", Map.of("name", "John"), 2, "third failure");
        doThrow(new RuntimeException("SMTP still down")).when(emailService).doSendEmail(anyString(), anyString(), anyString(), anyMap());

        consumer.handleEmailRetry(maxRetryEvent);

        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), (Object) any());
    }
}
