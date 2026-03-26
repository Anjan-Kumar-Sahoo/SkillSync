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
public class ReviewEventConsumer {

    private final NotificationCommandService notificationCommandService;

    @RabbitListener(queues = RabbitMQConfig.REVIEW_NOTIFICATION_SUBMITTED_QUEUE)
    public void handleReviewSubmitted(Map<String, Object> event) {
        Long mentorId = toLong(event.get("mentorId"));
        int rating = ((Number) event.get("rating")).intValue();
        notificationCommandService.createAndPush(mentorId, "REVIEW_SUBMITTED",
                "New Review Received",
                "You received a new " + rating + "-star review. Check your profile for details!");
        log.info("Processed REVIEW_SUBMITTED event for mentor {}", mentorId);
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
