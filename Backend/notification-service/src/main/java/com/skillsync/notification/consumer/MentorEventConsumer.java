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
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Role", "Mentor");
            details.put("Status", "Approved");
            details.put("Next Step", "Start managing sessions and availability");

            emailService.sendEmail(
                user.email(),
                "Welcome to SkillSync Mentors!",
                "system-email",
                Map.of(
                    "recipientName", displayName(user),
                    "title", "Welcome to SkillSync Mentors",
                    "preheader", "Your mentor application is now approved.",
                    "primaryMessage", "You are now live as a mentor on SkillSync.",
                    "detailsTitle", "Important details",
                    "detailsHtml", emailService.buildDetailsHtml(details),
                    "actionText", "Open Mentor Dashboard",
                    "actionUrl", appBaseUrl + "/mentor",
                    "footerNote", "Keep your availability updated so learners can book quickly."
                )
            );
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

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Status", "Not approved");
            details.put("Reason", reason != null && !reason.isBlank() ? reason : "Not specified");
            details.put("Next Step", "Update your profile and re-apply");

            emailService.sendEmail(
                    user.email(),
                    "Mentor Application Update - SkillSync",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "Mentor Application Update",
                            "preheader", "Your mentor application was reviewed.",
                            "primaryMessage", "Your current submission was not approved this time.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "Open SkillSync",
                            "actionUrl", appBaseUrl + "/profile",
                            "footerNote", "You can improve your profile and submit another application anytime."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send mentor rejection email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed MENTOR_REJECTED event for user {}", userId);
    }

    @RabbitListener(queues = RabbitMQConfig.MENTOR_NOTIFICATION_PROMOTED_QUEUE)
    public void handleMentorPromoted(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));

        notificationCommandService.createAndPush(userId, "MENTOR_PROMOTED",
                "You are now a Mentor",
                "Congratulations. Your role has been promoted to mentor and mentor access is now active.");

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Role", "Mentor");
            details.put("Access", "Mentor dashboard is now enabled");
            details.put("Next steps", "Set availability, complete your profile, and start accepting sessions");

            emailService.sendEmail(
                    user.email(),
                    "You are now a Mentor",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "You are now a Mentor",
                            "preheader", "Mentor access has been granted.",
                            "primaryMessage", "Congratulations. You now have full mentor access on SkillSync.",
                            "detailsTitle", "What to do next",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "Open Mentor Dashboard",
                            "actionUrl", appBaseUrl + "/mentor",
                            "footerNote", "Stay active and keep your availability up to date to receive more bookings."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send mentor promotion email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed MENTOR_PROMOTED event for user {}", userId);
    }

    @RabbitListener(queues = RabbitMQConfig.MENTOR_NOTIFICATION_DEMOTED_QUEUE)
    public void handleMentorDemoted(Map<String, Object> event) {
        Long userId = toLong(event.get("userId"));
        String reason = (String) event.get("reason");

        notificationCommandService.createAndPush(userId, "MENTOR_DEMOTED",
                "Role Updated",
                "Your role has been changed to learner by admin. You may re-apply for mentor access.");

        try {
            UserSummary user = authServiceClient.getUserById(userId);
            Map<String, String> details = new LinkedHashMap<>();
            details.put("Current role", "Learner");
            details.put("Change reason", reason != null && !reason.isBlank() ? reason : "Updated by admin");
            details.put("Re-apply", "You can submit a fresh mentor application from your profile");

            emailService.sendEmail(
                    user.email(),
                    "Role Updated",
                    "system-email",
                    Map.of(
                            "recipientName", displayName(user),
                            "title", "Role Updated",
                            "preheader", "Your SkillSync role has changed.",
                            "primaryMessage", "Your role has been updated to learner. You can re-apply for mentor access anytime.",
                            "detailsTitle", "Important details",
                            "detailsHtml", emailService.buildDetailsHtml(details),
                            "actionText", "Open Profile",
                            "actionUrl", appBaseUrl + "/profile",
                            "footerNote", "If you need help with re-application, contact support."
                    )
            );
        } catch (Exception e) {
            log.error("Failed to send mentor demotion email to user {}: {}", userId, e.getMessage());
        }

        log.info("Processed MENTOR_DEMOTED event for user {}", userId);
    }

    private String displayName(UserSummary user) {
        String first = user.firstName() != null ? user.firstName().trim() : "";
        String last = user.lastName() != null ? user.lastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.email() : full;
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
