package com.skillsync.user.consumer;

import com.skillsync.user.service.command.MentorCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Event-driven cache sync consumer.
 * Listens for ReviewSubmittedEvent from session-service
 * to update mentor ratings and invalidate cache.
 *
 * Idempotent: uses database upsert (avgRating/totalReviews are recalculated,
 * so duplicate events produce the same result).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReviewEventCacheSyncConsumer {

    private final MentorCommandService mentorCommandService;

    @RabbitListener(queues = "user.review.submitted.queue")
    public void handleReviewSubmitted(Map<String, Object> event) {
        try {
            Long mentorId = toLong(event.get("mentorId"));
            Object avgRatingValue = event.get("avgRating");
            if (avgRatingValue == null) {
                // Backward compatibility: session-service currently emits newAvgRating.
                avgRatingValue = event.get("newAvgRating");
            }
            if (!(avgRatingValue instanceof Number)) {
                throw new IllegalArgumentException("Missing avgRating/newAvgRating in review event payload");
            }
            double avgRating = ((Number) avgRatingValue).doubleValue();
            int totalReviews = ((Number) event.get("totalReviews")).intValue();
            Long totalSessions = null;
            Object totalSessionsValue = event.get("totalSessions");
            if (totalSessionsValue instanceof Number totalSessionsNumber) {
                totalSessions = totalSessionsNumber.longValue();
            }

            // Idempotent: recalculated avg/total from source; safe to replay
            mentorCommandService.updateMentorMetrics(mentorId, avgRating, totalReviews, totalSessions);
            log.info("[CACHE-SYNC] Updated mentor {} metrics: avg={}, totalReviews={}, totalSessions={}",
                    mentorId, avgRating, totalReviews, totalSessions);
        } catch (Exception e) {
            log.error("[CACHE-SYNC] Failed to process review event for cache sync: {}", e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
