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
public class MentorEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.base-url:https://skillsync.mraks.dev}")
    private String appBaseUrl;

    @RabbitListener(queues = RabbitMQConfig.MENTOR_NOTIFICATION_APPROVED_QUEUE)
    public void handleMentorApproved(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        notificationCommandService.createAndPush(userId, "MENTOR_APPROVED",
                "Mentor Application Approved!",
                "Congratulations! Your mentor application has been approved. You can now start accepting session requests.");

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            emailService.sendEmail(user.email(), "Welcome to SkillSync Mentors!", "mentor-approved",
                    Map.of("recipientName", user.firstName(),
                            "actionUrl", appBaseUrl + "/mentor/dashboard"));
        } catch (Exception e) {
            log.error("Failed to send mentor approval email to user {}: {}", userId, e.getMessage());
        }
        log.info("Processed MENTOR_APPROVED event for user {}", userId);
    }

    @RabbitListener(queues = RabbitMQConfig.MENTOR_NOTIFICATION_REJECTED_QUEUE)
    public void handleMentorRejected(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String reason = (String) event.get("reason");
        notificationCommandService.createAndPush(userId, "MENTOR_REJECTED",
                "Mentor Application Update",
                "Your mentor application was not approved. Reason: " + (reason != null ? reason : "Not specified"));
        log.info("Processed MENTOR_REJECTED event for user {}", userId);
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
