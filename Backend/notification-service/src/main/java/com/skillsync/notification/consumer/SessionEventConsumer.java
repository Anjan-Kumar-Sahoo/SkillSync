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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventConsumer {

    private final NotificationCommandService notificationCommandService;
    private final EmailService emailService;
    private final AuthServiceClient authServiceClient;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REQUESTED_QUEUE)
    public void handleSessionRequested(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        Long learnerId = toLong(event.get("learnerId"));
        String topic = (String) event.get("topic");

        notificationCommandService.createAndPush(mentorId, "SESSION_REQUESTED",
            "Session Requested",
            "New session request received");

        notificationCommandService.createAndPush(learnerId, "SESSION_REQUESTED_CONFIRMATION",
            "Session Requested",
            "Your session request has been sent");

        try {
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            UserSummary learner = authServiceClient.getUserById(learnerId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));
            String date = formatDate(sessionDateTime);
            String time = formatTime(sessionDateTime);

            emailService.sendEmail(
                mentor.email(),
                "New Session Request - SkillSync",
                "session-requested-mentor",
                Map.of(
                    "mentorName", displayName(mentor),
                    "learnerEmail", learner.email(),
                    "date", date,
                    "time", time,
                    "topic", topic != null ? topic : "Session"
                )
            );

            emailService.sendEmail(
                learner.email(),
                "Session Requested Successfully",
                "session-requested-learner",
                Map.of(
                    "learnerName", displayName(learner),
                    "mentorEmail", mentor.email(),
                    "date", date,
                    "time", time,
                    "topic", topic != null ? topic : "Session"
                )
            );
        } catch (Exception e) {
            log.error("Failed to send session requested emails for mentor {} and learner {}: {}",
                mentorId, learnerId, e.getMessage());
        }
        log.info("Processed SESSION_REQUESTED event for mentor {} and learner {}", mentorId, learnerId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_ACCEPTED_QUEUE)
    public void handleSessionAccepted(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        Long mentorId = toLong(event.get("mentorId"));
        String topic = (String) event.get("topic");

        notificationCommandService.createAndPush(learnerId, "SESSION_APPROVED",
            "Session Approved",
            "Your session has been approved");

        try {
            UserSummary learner = authServiceClient.getUserById(learnerId);
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));
            String date = formatDate(sessionDateTime);
            String time = formatTime(sessionDateTime);

            emailService.sendEmail(
                learner.email(),
                "Session Approved - SkillSync",
                "session-approved-learner",
                Map.of(
                    "learnerName", displayName(learner),
                    "mentorEmail", mentor.email(),
                    "date", date,
                    "time", time,
                    "topic", topic != null ? topic : "Session"
                )
            );
        } catch (Exception e) {
            log.error("Failed to send session accepted email to learner {}: {}", learnerId, e.getMessage());
        }

        log.info("Processed SESSION_ACCEPTED event for learner {} (mentor {} informed only at request stage)",
            learnerId, mentorId);
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

    private LocalDateTime parseDateTime(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "TBD";
        }
        return DATE_FORMATTER.format(dateTime);
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "TBD";
        }
        return TIME_FORMATTER.format(dateTime);
    }

    private String displayName(UserSummary user) {
        String first = user.firstName() != null ? user.firstName().trim() : "";
        String last = user.lastName() != null ? user.lastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.email() : full;
    }
}
