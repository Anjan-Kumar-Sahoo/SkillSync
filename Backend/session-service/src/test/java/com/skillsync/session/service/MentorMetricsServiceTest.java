package com.skillsync.session.service;

import com.skillsync.session.dto.MentorMetricsResponse;
import com.skillsync.session.dto.MentorRatingSummary;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorMetricsServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private ReviewRepository reviewRepository;

    @InjectMocks private MentorMetricsService mentorMetricsService;

    @Test
    @DisplayName("calculateMentorMetrics — computes blended average rating")
    void shouldCalculateMetrics() {
        when(sessionRepository.countByMentorIdAndStatus(100L, SessionStatus.COMPLETED)).thenReturn(10L);
        when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(100L, SessionStatus.COMPLETED)).thenReturn(2L);
        when(reviewRepository.countByMentorId(100L)).thenReturn(8L);
        when(reviewRepository.calculateTotalRating(100L)).thenReturn(36.0);

        MentorMetricsResponse result = mentorMetricsService.calculateMentorMetrics(100L);

        assertEquals(100L, result.mentorId());
        assertEquals(10L, result.completedSessions());
        assertEquals(8, result.totalReviews());
        assertEquals(2L, result.defaultRatedSessions());
        assertFalse(result.newMentor());
        // (36.0 + 2*2.5) / 10 = 41.0 / 10 = 4.10
        assertEquals(4.10, result.averageRating());
    }

    @Test
    @DisplayName("calculateMentorMetrics — new mentor with zero sessions")
    void shouldHandleZeroSessions() {
        when(sessionRepository.countByMentorIdAndStatus(100L, SessionStatus.COMPLETED)).thenReturn(0L);
        when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(100L, SessionStatus.COMPLETED)).thenReturn(0L);
        when(reviewRepository.countByMentorId(100L)).thenReturn(0L);

        MentorMetricsResponse result = mentorMetricsService.calculateMentorMetrics(100L);

        assertEquals(0.0, result.averageRating());
        assertTrue(result.newMentor());
    }

    @Test
    @DisplayName("calculateMentorMetrics — all sessions have explicit reviews")
    void shouldHandleAllExplicitReviews() {
        when(sessionRepository.countByMentorIdAndStatus(100L, SessionStatus.COMPLETED)).thenReturn(5L);
        when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(100L, SessionStatus.COMPLETED)).thenReturn(0L);
        when(reviewRepository.countByMentorId(100L)).thenReturn(5L);
        when(reviewRepository.calculateTotalRating(100L)).thenReturn(22.5);

        MentorMetricsResponse result = mentorMetricsService.calculateMentorMetrics(100L);

        // 22.5 / 5 = 4.50
        assertEquals(4.50, result.averageRating());
    }

    @Test
    @DisplayName("calculateMentorRatingSummary — includes distribution")
    void shouldCalculateRatingSummary() {
        when(sessionRepository.countByMentorIdAndStatus(100L, SessionStatus.COMPLETED)).thenReturn(5L);
        when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(100L, SessionStatus.COMPLETED)).thenReturn(0L);
        when(reviewRepository.countByMentorId(100L)).thenReturn(5L);
        when(reviewRepository.calculateTotalRating(100L)).thenReturn(22.5);
        when(reviewRepository.getRatingDistribution(100L)).thenReturn(List.of(
                new Object[]{5, 3L},
                new Object[]{4, 2L}
        ));

        MentorRatingSummary result = mentorMetricsService.calculateMentorRatingSummary(100L);

        assertEquals(4.50, result.averageRating());
        assertEquals(3, result.ratingDistribution().get(5));
        assertEquals(2, result.ratingDistribution().get(4));
    }
}
