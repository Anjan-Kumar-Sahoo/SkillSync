package com.skillsync.notification.consumer;

import com.skillsync.notification.dto.UserSummary;
import com.skillsync.notification.feign.AuthServiceClient;
import com.skillsync.notification.service.EmailService;
import com.skillsync.notification.service.command.NotificationCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;
    @InjectMocks private PaymentEventConsumer consumer;

    private UserSummary testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSummary(1L, "user@test.com", "LEARNER", "John", "Doe");
        
        try {
            var field = PaymentEventConsumer.class.getDeclaredField("appBaseUrl");
            field.setAccessible(true);
            field.set(consumer, "https://test.skillsync.dev");
        } catch (Exception ignored) {}

        lenient().when(emailService.buildDetailsHtml(any())).thenReturn("<html>Details</html>");
    }

    @Test @DisplayName("handlePaymentSuccess - Success")
    void handlePaymentSuccess() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-123");
        event.put("amount", 50000L); // 500.00
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentSuccess(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_SUCCESS"), anyString(), contains("session booking"));
        verify(emailService).sendEmail(eq("user@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handlePaymentFailed - Success")
    void handlePaymentFailed() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "OTHER");
        event.put("orderId", "ORD-124");
        event.put("amount", 1000L);
        event.put("compensationReason", "Insufficient funds");
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentFailed(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_FAILED"), anyString(), contains("ORD-124"));
        verify(emailService).sendEmail(eq("user@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handlePaymentCompensated - Success")
    void handlePaymentCompensated() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-125");
        event.put("amount", 2000L);
        event.put("compensationReason", "Timeout during booking");
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentCompensated(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_COMPENSATED"), anyString(), contains("ORD-125"));
        verify(emailService).sendEmail(eq("user@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("Handle email error gracefully")
    void handleEmailError() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-123");
        event.put("amount", 50000L);
        when(authServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Auth service down"));

        consumer.handlePaymentSuccess(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_SUCCESS"), anyString(), anyString());
    }
}
