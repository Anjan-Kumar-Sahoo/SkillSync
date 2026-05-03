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
class PaymentEventConsumerCoverageTest {

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

    @Test @DisplayName("handlePaymentSuccess - Default case branch")
    void handlePaymentSuccessDefault() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "UNKNOWN_TYPE");
        event.put("orderId", "ORD-999");
        event.put("amount", 1000L);
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentSuccess(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_SUCCESS"), anyString(), contains("payment was successful"));
    }

    @Test @DisplayName("handlePaymentFailed - Default case branch")
    void handlePaymentFailedDefault() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "MISC_FEE");
        event.put("orderId", "ORD-888");
        event.put("amount", 2000L);
        event.put("compensationReason", "null"); // Should trigger normalizeReason "null" case
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentFailed(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_FAILED"), anyString(), contains("could not be verified"));
    }

    @Test @DisplayName("handlePaymentCompensated - Default case branch")
    void handlePaymentCompensatedDefault() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "WALLET_TOPUP");
        event.put("orderId", "ORD-777");
        event.put("amount", 5000L);
        event.put("compensationReason", ""); // Should trigger normalizeReason empty case
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentCompensated(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_COMPENSATED"), anyString(), contains("processing issue occurred"));
    }

    @Test @DisplayName("displayName branches - null names")
    void displayNameBranches() {
        UserSummary userWithNoNames = new UserSummary(2L, "anon@test.com", "LEARNER", null, "  ");
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 2L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-000");
        event.put("amount", 0L);
        
        when(authServiceClient.getUserById(2L)).thenReturn(userWithNoNames);

        consumer.handlePaymentSuccess(event);

        verify(emailService).sendEmail(eq("anon@test.com"), anyString(), anyString(), argThat(map -> 
            map.get("recipientName").equals("anon@test.com")
        ));
    }

    @Test @DisplayName("toLong branches - String userId")
    void toLongBranches() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", "123"); // String case
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-123");
        event.put("amount", 100L);
        
        when(authServiceClient.getUserById(123L)).thenReturn(testUser);

        consumer.handlePaymentSuccess(event);

        verify(notificationCommandService).createAndPush(eq(123L), anyString(), anyString(), anyString());
    }

    @Test @DisplayName("handlePaymentFailed - Email service error")
    void handlePaymentFailedEmailError() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-ERR");
        event.put("amount", 1000L);
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);
        lenient().doThrow(new RuntimeException("SMTP Down")).when(emailService).sendEmail(any(), any(), any(), any());

        consumer.handlePaymentFailed(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_FAILED"), anyString(), anyString());
    }

    @Test @DisplayName("handlePaymentCompensated - Email service error")
    void handlePaymentCompensatedEmailError() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-ERR2");
        event.put("amount", 1000L);
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);
        lenient().doThrow(new RuntimeException("SMTP Down")).when(emailService).sendEmail(any(), any(), any(), any());

        consumer.handlePaymentCompensated(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_COMPENSATED"), anyString(), anyString());
    }
}
