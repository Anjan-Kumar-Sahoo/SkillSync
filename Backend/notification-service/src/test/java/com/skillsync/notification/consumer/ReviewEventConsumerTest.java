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
class ReviewEventConsumerTest {

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;
    @InjectMocks private ReviewEventConsumer consumer;

    private UserSummary testUser;

    @BeforeEach
    void setUp() {
        testUser = new UserSummary(1L, "mentor@test.com", "MENTOR", "John", "Doe");
        
        try {
            var field = ReviewEventConsumer.class.getDeclaredField("appBaseUrl");
            field.setAccessible(true);
            field.set(consumer, "https://test.skillsync.dev");
        } catch (Exception ignored) {}

        lenient().when(emailService.buildDetailsHtml(any())).thenReturn("<html>Details</html>");
    }

    @Test @DisplayName("handleReviewSubmitted - Success")
    void handleReviewSubmitted() {
        Map<String, Object> event = new HashMap<>();
        event.put("mentorId", 1L);
        event.put("rating", 5);
        event.put("comment", "Great session!");
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleReviewSubmitted(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("REVIEW_SUBMITTED"), anyString(), contains("5-star"));
        verify(emailService).sendEmail(eq("mentor@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleReviewSubmitted - Null comment")
    void handleReviewSubmittedNullComment() {
        Map<String, Object> event = new HashMap<>();
        event.put("mentorId", 1L);
        event.put("rating", 4);
        event.put("comment", null);
        when(authServiceClient.getUserById(1L)).thenReturn(testUser);

        consumer.handleReviewSubmitted(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("REVIEW_SUBMITTED"), anyString(), anyString());
        verify(emailService).sendEmail(anyString(), anyString(), anyString(), anyMap());
    }

    @Test @DisplayName("Handle email error gracefully")
    void handleEmailError() {
        Map<String, Object> event = new HashMap<>();
        event.put("mentorId", 1L);
        event.put("rating", 5);
        when(authServiceClient.getUserById(1L)).thenThrow(new RuntimeException("Auth service down"));

        consumer.handleReviewSubmitted(event);

        verify(notificationCommandService).createAndPush(eq(1L), eq("REVIEW_SUBMITTED"), anyString(), anyString());
    }
}
