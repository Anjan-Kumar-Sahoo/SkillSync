package com.skillsync.session.service;

import com.skillsync.session.dto.MentorMetricsResponse;
import com.skillsync.session.dto.MentorRatingSummary;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorMetricsServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private ReviewRepository reviewRepository;
    @InjectMocks private MentorMetricsService service;

    @Nested @DisplayName("calculateMentorMetrics")
    class CalculateMetricsTests {
        @Test @DisplayName("No completed sessions - new mentor")
        void noSessions() {
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(reviewRepository.countByMentorId(10L)).thenReturn(0L);

            MentorMetricsResponse resp = service.calculateMentorMetrics(10L);
            assertEquals(0.0, resp.averageRating());
            assertTrue(resp.newMentor());
            assertEquals(0, resp.completedSessions());
        }

        @Test @DisplayName("With sessions and reviews - computed average")
        void withSessions() {
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(10L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(2L);
            when(reviewRepository.countByMentorId(10L)).thenReturn(8L);
            when(reviewRepository.calculateTotalRating(10L)).thenReturn(32.0);

            MentorMetricsResponse resp = service.calculateMentorMetrics(10L);
            assertFalse(resp.newMentor());
            assertEquals(10, resp.completedSessions());
            assertEquals(8, resp.totalReviews());
            // (32.0 + 2*2.5) / 10 = 37/10 = 3.7
            assertEquals(3.7, resp.averageRating());
        }
    }

    @Nested @DisplayName("calculateMentorRatingSummary")
    class CalculateSummaryTests {
        @Test @DisplayName("Includes rating distribution")
        void withDistribution() {
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(5L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(reviewRepository.countByMentorId(10L)).thenReturn(5L);
            when(reviewRepository.calculateTotalRating(10L)).thenReturn(20.0);
            when(reviewRepository.getRatingDistribution(10L)).thenReturn(
                    List.of(new Object[]{5, 3L}, new Object[]{4, 2L}));

            MentorRatingSummary summary = service.calculateMentorRatingSummary(10L);
            assertEquals(2, summary.ratingDistribution().size());
            assertEquals(3, summary.ratingDistribution().get(5));
            assertEquals(2, summary.ratingDistribution().get(4));
        }

        @Test @DisplayName("Empty distribution")
        void emptyDistribution() {
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(reviewRepository.countByMentorId(10L)).thenReturn(0L);
            when(reviewRepository.getRatingDistribution(10L)).thenReturn(List.of());

            MentorRatingSummary summary = service.calculateMentorRatingSummary(10L);
            assertTrue(summary.ratingDistribution().isEmpty());
        }
    }
}
