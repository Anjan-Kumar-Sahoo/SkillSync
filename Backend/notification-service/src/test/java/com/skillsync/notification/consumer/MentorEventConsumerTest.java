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
class MentorEventConsumerTest {

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;
    @InjectMocks private MentorEventConsumer consumer;

    private UserSummary testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSummary(1L, "mentor@test.com", "MENTOR", "John", "Doe");
        
        // Set appBaseUrl via reflection
        try {
            var field = MentorEventConsumer.class.getDeclaredField("appBaseUrl");
            field.setAccessible(true);
            field.set(consumer, "https://test.skillsync.dev");
        } catch (Exception ignored) {}

        lenient().when(emailService.buildDetailsHtml(any())).thenReturn("<html>Details</html>");
    }

    @Test @DisplayName("handleMentorApproved - Success")
    void handleMentorApproved() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleMentorApproved(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("MENTOR_APPROVED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("mentor@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleMentorRejected - Success")
    void handleMentorRejected() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("reason", "Incomplete profile");
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleMentorRejected(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("MENTOR_REJECTED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("mentor@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleMentorPromoted - Success")
    void handleMentorPromoted() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleMentorPromoted(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("MENTOR_PROMOTED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("mentor@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleMentorDemoted - Success")
    void handleMentorDemoted() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        event.put("reason", "Violation of terms");
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleMentorDemoted(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("MENTOR_DEMOTED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("mentor@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("Handle email error gracefully")
    void handleEmailError() {
        Map<String, Object> event = new HashMap<>();
        event.put("userId", 1L);
        when(authServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Auth service down"));

        consumer.handleMentorApproved(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("MENTOR_APPROVED"), anyString(), anyString());
        // Verify that it doesn't throw and logs error (indirectly verified by test not failing)
    }
}
