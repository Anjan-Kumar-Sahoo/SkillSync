package com.skillsync.user.consumer;

import com.skillsync.cache.CacheService;
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
            double avgRating = ((Number) event.get("avgRating")).doubleValue();
            int totalReviews = ((Number) event.get("totalReviews")).intValue();

            // Idempotent: recalculated avg/total from source; safe to replay
            mentorCommandService.updateAvgRating(mentorId, avgRating, totalReviews);
            log.info("[CACHE-SYNC] Updated mentor {} rating: avg={}, total={} (versioned keys invalidated)",
                    mentorId, avgRating, totalReviews);
        } catch (Exception e) {
            log.error("[CACHE-SYNC] Failed to process review event for cache sync: {}", e.getMessage());
        }
    }

    private Long toLong(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }
}
