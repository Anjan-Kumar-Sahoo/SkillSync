package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.dto.UserSummary;
import com.skillsync.notification.feign.AuthServiceClient;
import com.skillsync.notification.service.EmailService;
import com.skillsync.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_NOTIFICATION_SUBMITTED_QUEUE)
    public void handleReviewSubmitted(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        int rating = ((Number) event.get("rating")).intValue();
        String comment = (String) event.getOrDefault("comment", "No comment provided.");

        notificationCommandService.createAndPush(mentorId, "REVIEW_SUBMITTED",
                "New Review Received",
                "You received a new " + rating + "-star review. Check your profile for details!");

        // Email notification
        try {
            UserSummary user = authServiceClient.getUserById(mentorId);
            emailService.sendEmail(user.email(), "You received a new review!", "review-submitted",
                    Map.of("recipientName", user.firstName(),
                            "sessionTitle", "Recent Mentorship Session",
                            "rating", rating + ".0",
                            "comment", comment,
                            "actionUrl", "http://localhost/mentor/profile"));
        } catch (Exception e) {
            log.error("Failed to send review email to mentor {}: {}", mentorId, e.getMessage());
        }
        log.info("Processed REVIEW_SUBMITTED event for mentor {}", mentorId);
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
