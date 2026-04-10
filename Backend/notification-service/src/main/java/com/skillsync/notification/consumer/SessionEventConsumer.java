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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a");

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REQUESTED_QUEUE)
    public void handleSessionRequested(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        Long learnerId = toLong(event.get("learnerId"));
        String topic = defaultTopic(event.get("topic"));

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

            Map<String, String> mentorDetails = new LinkedHashMap<>();
            mentorDetails.put("Learner", displayName(learner));
            mentorDetails.put("Date", date);
            mentorDetails.put("Time", time);
            mentorDetails.put("Topic", topic);
            sendSystemEmail(
                    mentor,
                    "New Session Request - SkillSync",
                    "Session Request Received",
                    "A learner requested a mentoring session.",
                    "A new session request needs your response.",
                    mentorDetails,
                    "Review Request",
                    appBaseUrl + "/sessions",
                    "Review and respond quickly to keep your response score high."
            );

            Map<String, String> learnerDetails = new LinkedHashMap<>();
            learnerDetails.put("Mentor", displayName(mentor));
            learnerDetails.put("Date", date);
            learnerDetails.put("Time", time);
            learnerDetails.put("Topic", topic);
            sendSystemEmail(
                    learner,
                    "Session Booked - SkillSync",
                    "Session Booked",
                    "Your booking request has been created.",
                    "Your request is submitted and waiting for mentor approval.",
                    learnerDetails,
                    "Track Booking",
                    appBaseUrl + "/sessions",
                    "You will receive another update once the mentor responds."
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
        String topic = defaultTopic(event.get("topic"));

        notificationCommandService.createAndPush(learnerId, "SESSION_APPROVED",
                "Session Approved",
                "Your session has been approved");

        try {
            UserSummary learner = authServiceClient.getUserById(learnerId);
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));

            Map<String, String> details = new LinkedHashMap<>();
            details.put("Mentor", displayName(mentor));
            details.put("Date", formatDate(sessionDateTime));
            details.put("Time", formatTime(sessionDateTime));
            details.put("Topic", topic);

            sendSystemEmail(
                    learner,
                    "Session Approved - SkillSync",
                    "Session Confirmed",
                    "Your mentor approved the session request.",
                    "Your session is confirmed and ready. Join on time from your sessions page.",
                    details,
                    "Open My Sessions",
                    appBaseUrl + "/sessions",
                    "Need to reschedule? Cancel in advance to avoid disruptions."
            );
        } catch (Exception e) {
            log.error("Failed to send session approved email to learner {}: {}", learnerId, e.getMessage());
        }

        log.info("Processed SESSION_ACCEPTED event for learner {}", learnerId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REJECTED_QUEUE)
    public void handleSessionRejected(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        Long mentorId = toLong(event.get("mentorId"));
        String topic = defaultTopic(event.get("topic"));
        String reason = normalizeReason(event.get("cancelReason"));

        notificationCommandService.createAndPush(learnerId, "SESSION_REJECTED",
                "Session Rejected",
                "Your session request for '" + topic + "' has been rejected.");

        try {
            UserSummary learner = authServiceClient.getUserById(learnerId);
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));

            Map<String, String> details = new LinkedHashMap<>();
            details.put("Mentor", displayName(mentor));
            details.put("Date", formatDate(sessionDateTime));
            details.put("Time", formatTime(sessionDateTime));
            details.put("Topic", topic);
            details.put("Reason", reason);

            sendSystemEmail(
                    learner,
                    "Session Rejected - SkillSync",
                    "Session Request Declined",
                    "Your mentor could not accept this session.",
                    "You can book another slot with this mentor or discover another expert.",
                    details,
                    "Find Mentors",
                    appBaseUrl + "/mentors",
                    "Keep learning momentum by booking your next session today."
            );
        } catch (Exception e) {
            log.error("Failed to send session rejected email to learner {}: {}", learnerId, e.getMessage());
        }

        log.info("Processed SESSION_REJECTED event for learner {}", learnerId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_CANCELLED_QUEUE)
    public void handleSessionCancelled(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        Long learnerId = toLong(event.get("learnerId"));
        String topic = defaultTopic(event.get("topic"));
        String reason = normalizeReason(event.get("cancelReason"));

        notificationCommandService.createAndPush(mentorId, "SESSION_CANCELLED",
                "Session Cancelled",
                "Session for '" + topic + "' has been cancelled.");
        notificationCommandService.createAndPush(learnerId, "SESSION_CANCELLED",
                "Session Cancelled",
                "Session for '" + topic + "' has been cancelled.");

        try {
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            UserSummary learner = authServiceClient.getUserById(learnerId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));
            String date = formatDate(sessionDateTime);
            String time = formatTime(sessionDateTime);

            Map<String, String> learnerDetails = new LinkedHashMap<>();
            learnerDetails.put("Mentor", displayName(mentor));
            learnerDetails.put("Date", date);
            learnerDetails.put("Time", time);
            learnerDetails.put("Topic", topic);
            learnerDetails.put("Reason", reason);
            sendSystemEmail(
                    learner,
                    "Session Cancelled - SkillSync",
                    "Session Cancelled",
                    "Your booked session has been cancelled.",
                    "Use the details below to understand what changed.",
                    learnerDetails,
                    "Book Another Session",
                    appBaseUrl + "/mentors",
                    "You can rebook anytime from your mentor discovery page."
            );

            Map<String, String> mentorDetails = new LinkedHashMap<>();
            mentorDetails.put("Learner", displayName(learner));
            mentorDetails.put("Date", date);
            mentorDetails.put("Time", time);
            mentorDetails.put("Topic", topic);
            mentorDetails.put("Reason", reason);
            sendSystemEmail(
                    mentor,
                    "Session Cancelled - SkillSync",
                    "Session Cancelled",
                    "One of your scheduled sessions has been cancelled.",
                    "Use the details below for quick reference.",
                    mentorDetails,
                    "View Sessions",
                    appBaseUrl + "/sessions",
                    "Consider opening another slot to keep your schedule full."
            );
        } catch (Exception e) {
            log.error("Failed to send session cancelled emails for mentor {} and learner {}: {}",
                    mentorId, learnerId, e.getMessage());
        }

        log.info("Processed SESSION_CANCELLED event for mentor {} and learner {}", mentorId, learnerId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_COMPLETED_QUEUE)
    public void handleSessionCompleted(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        Long mentorId = toLong(event.get("mentorId"));
        String topic = defaultTopic(event.get("topic"));

        notificationCommandService.createAndPush(learnerId, "SESSION_COMPLETED",
                "Session Completed!",
                "Your session '" + topic + "' is complete. Please leave a review!");

        try {
            UserSummary learner = authServiceClient.getUserById(learnerId);
            UserSummary mentor = authServiceClient.getUserById(mentorId);
            LocalDateTime sessionDateTime = parseDateTime(event.get("sessionDateTime"));
            String date = formatDate(sessionDateTime);
            String time = formatTime(sessionDateTime);

            Map<String, String> learnerDetails = new LinkedHashMap<>();
            learnerDetails.put("Mentor", displayName(mentor));
            learnerDetails.put("Date", date);
            learnerDetails.put("Time", time);
            learnerDetails.put("Topic", topic);
            sendSystemEmail(
                    learner,
                    "Session Completed - SkillSync",
                    "Session Completed",
                    "Your session finished successfully.",
                    "Share a review to help mentors and future learners.",
                    learnerDetails,
                    "Leave Review",
                    appBaseUrl + "/sessions",
                    "Your feedback helps improve mentorship quality across SkillSync."
            );

            Map<String, String> mentorDetails = new LinkedHashMap<>();
            mentorDetails.put("Learner", displayName(learner));
            mentorDetails.put("Date", date);
            mentorDetails.put("Time", time);
            mentorDetails.put("Topic", topic);
            sendSystemEmail(
                    mentor,
                    "Session Completed - SkillSync",
                    "Session Completed",
                    "This session has been marked as completed.",
                    "Your earnings and completed-session stats will be updated accordingly.",
                    mentorDetails,
                    "Open Dashboard",
                    appBaseUrl + "/mentor",
                    "Great mentoring drives long-term learner success."
            );
        } catch (Exception e) {
            log.error("Failed to send session completed emails for mentor {} and learner {}: {}",
                    mentorId, learnerId, e.getMessage());
        }

        log.info("Processed SESSION_COMPLETED event for mentor {} and learner {}", mentorId, learnerId);
    }

    private void sendSystemEmail(
            UserSummary recipient,
            String subject,
            String title,
            String preheader,
            String primaryMessage,
            Map<String, String> details,
            String actionText,
            String actionUrl,
            String footerNote
    ) {
        if (recipient == null || recipient.email() == null || recipient.email().isBlank()) {
            return;
        }

        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("recipientName", displayName(recipient));
        variables.put("title", title);
        variables.put("preheader", preheader);
        variables.put("primaryMessage", primaryMessage);
        variables.put("detailsTitle", "Important details");
        variables.put("detailsHtml", emailService.buildDetailsHtml(details));
        variables.put("actionText", actionText);
        variables.put("actionUrl", actionUrl);
        variables.put("footerNote", footerNote);

        emailService.sendEmail(recipient.email(), subject, "system-email", variables);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
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
        if (user == null) {
            return "there";
        }
        String first = user.firstName() != null ? user.firstName().trim() : "";
        String last = user.lastName() != null ? user.lastName().trim() : "";
        String full = (first + " " + last).trim();
        return full.isEmpty() ? user.email() : full;
    }

    private String defaultTopic(Object value) {
        if (value == null) {
            return "Session";
        }
        String topic = String.valueOf(value).trim();
        return topic.isEmpty() ? "Session" : topic;
    }

    private String normalizeReason(Object value) {
        if (value == null) {
            return "Not specified";
        }
        String reason = String.valueOf(value).trim();
        if (reason.isEmpty() || "null".equalsIgnoreCase(reason)) {
            return "Not specified";
        }
        return reason;
    }
}
