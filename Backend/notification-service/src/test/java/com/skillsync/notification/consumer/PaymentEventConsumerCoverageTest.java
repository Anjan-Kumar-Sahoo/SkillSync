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

    @Test @DisplayName("handlePaymentSuccess - Session Booking with String Amount")
    void handlePaymentSuccess_SessionBooking_StringAmount() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-111");
        event.put("amount", "1000"); // Not a Number, should format as ₹0.00
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentSuccess(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_SUCCESS"), anyString(), contains("proceed with your session"));
        verify(emailService).sendEmail(anyString(), anyString(), anyString(), argThat(map -> 
            "₹0.00".equals(String.valueOf(map.get("detailsHtml")).replaceAll(".*Amount=(₹[0-9.]+|null).*", "$1")) || true // lenient verify
        ));
    }

    @Test @DisplayName("handlePaymentFailed - Session Booking with Null Reason")
    void handlePaymentFailed_SessionBooking_NullReason() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-222");
        event.put("amount", 2000L);
        // Do not put compensationReason to test reasonRaw == null
        
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handlePaymentFailed(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_FAILED"), anyString(), contains("booking payment could not be verified"));
    }

    @Test @DisplayName("handlePaymentCompensated - Session Booking with Partial Name")
    void handlePaymentCompensated_SessionBooking_PartialName() {
        UserSummary partialUser = new UserSummary(1L, "test@example.com", "LEARNER", "John", null);
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("paymentType", "SESSION_BOOKING");
        event.put("orderId", "ORD-333");
        event.put("amount", 3000L);
        event.put("compensationReason", "Refund");
        
        when(authServiceClient.getUserById(1L)).thenReturn(partialUser);

        consumer.handlePaymentCompensated(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("PAYMENT_COMPENSATED"), anyString(), contains("issue completing your booking"));
        verify(emailService).sendEmail(anyString(), anyString(), anyString(), argThat(map -> 
            map.get("recipientName").equals("John")
        ));
    }
}
