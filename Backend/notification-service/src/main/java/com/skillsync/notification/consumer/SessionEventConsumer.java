package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.UserSummary;
import com.skillsync.notification.feign.AuthServiceClient;
import com.skillsync.notification.service.EmailService;
import com.skillsync.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.base-url:https://skillsync.mraks.dev}")
    private String appBaseUrl;

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REQUESTED_QUEUE)
    public void handleSessionRequested(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        String topic = (String) event.get("topic");
        notificationCommandService.createAndPush(mentorId, "SESSION_REQUESTED",
                "New Session Request",
                "You have a new session request for: " + topic);
        try {
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            emailService.sendEmail(mentor.email(), "New Session Request on SkillSync!", "session-booked",
                    Map.of("recipientName", mentor.firstName(),
                            "message", "A learner has requested a session for topic: '" + topic + "'. Please review and accept it from your dashboard.",
                            "actionUrl", appBaseUrl + "/mentor/sessions",
                            "actionText", "View Requests"));
        } catch (Exception e) {
            log.error("Failed to send session requested email to mentor {}: {}", mentorId, e.getMessage());
        }
        log.info("Processed SESSION_REQUESTED event for mentor {}", mentorId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_ACCEPTED_QUEUE)
    public void handleSessionAccepted(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        Long mentorId = toLong(event.get("mentorId"));
        String topic = (String) event.get("topic");

        notificationCommandService.createAndPush(learnerId, "SESSION_ACCEPTED",
                "Session Confirmed!",
                "Your session for '" + topic + "' is confirmed.");

        notificationCommandService.createAndPush(mentorId, "SESSION_ACCEPTED",
                "New Session Booked!",
                "A learner has booked and confirmed a session for '" + topic + "'.");

        // Email notification to Learner
        try {
            UserSummary learner = authServiceClient.getUserById(learnerId);
            emailService.sendEmail(learner.email(), "SkillSync Session Confirmed!", "session-booked",
                    Map.of("recipientName", learner.firstName(),
                            "message", "Your session for '" + topic + "' has been successfully booked and paid for.",
                            "actionUrl", appBaseUrl + "/learner/sessions",
                            "actionText", "View Sessions"));
        } catch (Exception e) {
            log.error("Failed to send session accepted email to learner {}: {}", learnerId, e.getMessage());
        }

        // Email notification to Mentor
        try {
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            emailService.sendEmail(mentor.email(), "New Session Booked on SkillSync!", "session-booked",
                    Map.of("recipientName", mentor.firstName(),
                            "message", "A learner has successfully booked and paid for a session with you on topic: '" + topic + "'.",
                            "actionUrl", appBaseUrl + "/mentor/sessions",
                            "actionText", "View Schedule"));
        } catch (Exception e) {
            log.error("Failed to send session accepted email to mentor {}: {}", mentorId, e.getMessage());
        }
        log.info("Processed SESSION_ACCEPTED event for learner {} and mentor {}", learnerId, mentorId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REJECTED_QUEUE)
    public void handleSessionRejected(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        String topic = (String) event.get("topic");
        notificationCommandService.createAndPush(learnerId, "SESSION_REJECTED",
                "Session Rejected",
                "Your session request for '" + topic + "' has been rejected.");
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_CANCELLED_QUEUE)
    public void handleSessionCancelled(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        Long learnerId = toLong(event.get("learnerId"));
        String topic = (String) event.get("topic");
        // Notify both parties
        notificationCommandService.createAndPush(mentorId, "SESSION_CANCELLED",
                "Session Cancelled", "Session for '" + topic + "' has been cancelled.");
        notificationCommandService.createAndPush(learnerId, "SESSION_CANCELLED",
                "Session Cancelled", "Session for '" + topic + "' has been cancelled.");
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_COMPLETED_QUEUE)
    public void handleSessionCompleted(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        String topic = (String) event.get("topic");
        notificationCommandService.createAndPush(learnerId, "SESSION_COMPLETED",
                "Session Completed!",
                "Your session '" + topic + "' is complete. Please leave a review!");
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
