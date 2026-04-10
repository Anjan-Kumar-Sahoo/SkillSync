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

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    @Value("${app.base-url:https://skillsync.mraks.dev}")
    private String appBaseUrl;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_NOTIFICATION_SUBMITTED_QUEUE)
    public void handleReviewSubmitted(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        int rating = ((Number) event.get("rating")).intValue();
        String comment = normalizeComment(event.get("comment"));

        notificationCommandService.createAndPush(mentorId, "REVIEW_SUBMITTED",
                "New Review Received",
                "You received a new " + rating + "-star review. Check your profile for details!");

        // Email notification
        try {
            UserSummary user = authServiceClient.getUserById(mentorId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Session", "Recent Mentorship Session");
            details.put("Rating", rating + "/5");
            details.put("Comment", comment);

            emailService.sendEmail(
                    user.email(),
                    "You received a new review!",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "New Review Received",
                            "preheader", "A learner left feedback on your mentoring session.",
                            "primaryMessage", "Great work. Check your latest review and keep the momentum going.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "View Reviews",
                            "actionUrl", appBaseUrl + "/mentor",
                            "footerNote", "Consistent high ratings improve your discoverability in mentor search."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send review email to mentor {}: {}", mentorId, e.getMessage());
        }
        log.info("Processed REVIEW_SUBMITTED event for mentor {}", mentorId);
    }

    private String displayName(UserSummary user) {
        String first = user.firstName() != null ? user.firstName().trim() : "";
        String last = user.lastName() != null ? user.lastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.email() : full;
    }

    private String normalizeComment(Object value) {
        if (value == null) {
            return "No comment provided.";
        }
        String comment = String.valueOf(value).trim();
        if (comment.isEmpty() || "null".equalsIgnoreCase(comment)) {
            return "No comment provided.";
        }
        return comment;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
