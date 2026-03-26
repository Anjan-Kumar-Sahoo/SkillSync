package com.skillsync.notification.consumer;

import com.skillsync.notification.config.RabbitMQConfig;
import com.skillsync.notification.service.command.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventConsumer {

    private final NotificationCommandService notificationCommandService;

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_REQUESTED_QUEUE)
    public void handleSessionRequested(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        String topic = (String) event.get("topic");
        notificationCommandService.createAndPush(mentorId, "SESSION_REQUESTED",
                "New Session Request",
                "You have a new session request for: " + topic);
        log.info("Processed SESSION_REQUESTED event for mentor {}", mentorId);
    }

    @RabbitListener(queues = RabbitMQConfig.SESSION_NOTIFICATION_ACCEPTED_QUEUE)
    public void handleSessionAccepted(Map<String, Object> event) {
        Long learnerId = toLong(event.get("learnerId"));
        String topic = (String) event.get("topic");
        notificationCommandService.createAndPush(learnerId, "SESSION_ACCEPTED",
                "Session Accepted!",
                "Your session request for '" + topic + "' has been accepted.");
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
