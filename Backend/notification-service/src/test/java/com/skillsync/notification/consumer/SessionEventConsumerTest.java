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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionEventConsumerTest {

    @Mock private NotificationCommandService notificationCommandService;
    @Mock private EmailService emailService;
    @Mock private AuthServiceClient authServiceClient;
    @InjectMocks private SessionEventConsumer consumer;

    private UserSummary testMentor;
    private UserSummary testLearner;

    @BeforeEach
    void setUp() {
        testMentor = new UserSummary(1L, "mentor@test.com", "MENTOR", "Mentor", "One");
        testLearner = new UserSummary(2L, "learner@test.com", "LEARNER", "Learner", "Two");
        
        try {
            var field = SessionEventConsumer.class.getDeclaredField("appBaseUrl");
            field.setAccessible(true);
            field.set(consumer, "https://test.skillsync.dev");
        } catch (Exception ignored) {}

        lenient().when(emailService.buildDetailsHtml(any())).thenReturn("<html>Details</html>");
    }

    @Test @DisplayName("handleSessionRequested - Success")
    void handleSessionRequested() {
        Map<String, Object> event = new HashMap<>();
        event.put("mentorId", 1L);
        event.put("learnerId", 2L);
        event.put("topic", "Java");
        event.put("sessionDateTime", LocalDateTime.now().toString());
        
        when(authServiceClient.getUserById(1L)).thenReturn(testMentor);
        when(authServiceClient.getUserById(2L)).thenReturn(testLearner);

        consumer.handleSessionRequested(event);

        verify(notificationCommandService, times(2)).createAndPush(anyLong(), anyString(), anyString(), anyString());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleSessionAccepted - Success")
    void handleSessionAccepted() {
        Map<String, Object> event = new HashMap<>();
        event.put("learnerId", 2L);
        event.put("mentorId", 1L);
        event.put("sessionDateTime", LocalDateTime.now().toString());
        
        when(authServiceClient.getUserById(2L)).thenReturn(testLearner);
        when(authServiceClient.getUserById(1L)).thenReturn(testMentor);

        consumer.handleSessionAccepted(event);

        verify(notificationCommandService).createAndPush(eq(2L), eq("SESSION_APPROVED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("learner@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleSessionRejected - Success")
    void handleSessionRejected() {
        Map<String, Object> event = new HashMap<>();
        event.put("learnerId", 2L);
        event.put("mentorId", 1L);
        event.put("cancelReason", "Busy");
        
        when(authServiceClient.getUserById(2L)).thenReturn(testLearner);
        when(authServiceClient.getUserById(1L)).thenReturn(testMentor);

        consumer.handleSessionRejected(event);

        verify(notificationCommandService).createAndPush(eq(2L), eq("SESSION_REJECTED"), anyString(), anyString());
        verify(emailService).sendEmail(eq("learner@test.com"), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleSessionCancelled - Success")
    void handleSessionCancelled() {
        Map<String, Object> event = new HashMap<>();
        event.put("mentorId", 1L);
        event.put("learnerId", 2L);
        
        when(authServiceClient.getUserById(1L)).thenReturn(testMentor);
        when(authServiceClient.getUserById(2L)).thenReturn(testLearner);

        consumer.handleSessionCancelled(event);

        verify(notificationCommandService, times(2)).createAndPush(anyLong(), anyString(), anyString(), anyString());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), eq("system-email"), anyMap());
    }

    @Test @DisplayName("handleSessionCompleted - Success")
    void handleSessionCompleted() {
        Map<String, Object> event = new HashMap<>();
        event.put("learnerId", 2L);
        event.put("mentorId", 1L);
        
        when(authServiceClient.getUserById(2L)).thenReturn(testLearner);
        when(authServiceClient.getUserById(1L)).thenReturn(testMentor);

        consumer.handleSessionCompleted(event);

        verify(notificationCommandService).createAndPush(eq(2L), eq("SESSION_COMPLETED"), anyString(), anyString());
        verify(emailService, times(2)).sendEmail(anyString(), anyString(), eq("system-email"), anyMap());
    }
}
