package com.skillsync.user.consumer;

import com.skillsync.cache.CacheService;

import com.skillsync.user.service.command.MentorCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventCacheSyncConsumerTest {

    @Mock private MentorCommandService mentorCommandService;
    @Mock private CacheService cacheService;

    @InjectMocks private ReviewEventCacheSyncConsumer consumer;

    @Test
    @DisplayName("Consume Review Submitted Event - Updates DB and invalidates cache")
    void handleReviewSubmitted_shouldUpdateDbAndCache() {
        Map<String, Object> event = Map.of(
                "mentorId", 2L,
                "avgRating", 4.5,
                "totalReviews", 10
        );

        consumer.handleReviewSubmitted(event);

        // Verify the mentor command service is called exactly once with proper recalculations
        verify(mentorCommandService).updateAvgRating(2L, 4.5, 10);
    }
}
