package com.skillsync.user.consumer;

import com.skillsync.user.service.command.MentorCommandService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewEventCacheSyncConsumerTest {

    @Mock private MentorCommandService mentorCommandService;

    @InjectMocks private ReviewEventCacheSyncConsumer consumer;

    @Test
    @DisplayName("Consume Review Submitted Event - supports avgRating field")
    void handleReviewSubmitted_shouldUpdateRating_usingAvgRatingField() {
        Map<String, Object> event = Map.of(
                "mentorId", 2L,
                "avgRating", 4.5,
                "totalReviews", 10
        );

        consumer.handleReviewSubmitted(event);

        verify(mentorCommandService).updateMentorMetrics(2L, 4.5, 10, null);
    }

    @Test
    @DisplayName("Consume Review Submitted Event - supports newAvgRating field")
    void handleReviewSubmitted_shouldUpdateRating_usingNewAvgRatingField() {
        Map<String, Object> event = Map.of(
                "mentorId", 7L,
                "newAvgRating", 4.8,
                "totalReviews", 16
        );

        consumer.handleReviewSubmitted(event);

        verify(mentorCommandService).updateMentorMetrics(7L, 4.8, 16, null);
    }
}
