package com.skillsync.session.service;

import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @InjectMocks private ReviewService service;

    private Session completedSession;
    private Review testReview;

    @BeforeEach
    void setUp() {
        completedSession = Session.builder()
                .id(1L).mentorId(10L).learnerId(20L)
                .status(SessionStatus.COMPLETED).build();
        testReview = Review.builder()
                .id(100L).sessionId(1L).mentorId(10L).reviewerId(20L)
                .rating(5).comment("Great").createdAt(LocalDateTime.now()).build();
    }

    @Nested @DisplayName("submitReview")
    class SubmitTests {
        @Test @DisplayName("Success")
        void success() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.save(any())).thenReturn(testReview);
            when(reviewRepository.calculateAverageRating(10L)).thenReturn(4.5);
            when(reviewRepository.countByMentorId(10L)).thenReturn(3L);

            CreateReviewRequest req = new CreateReviewRequest(1L, 5, "Great");
            ReviewResponse resp = service.submitReview(20L, req);
            assertNotNull(resp);
            assertEquals(5, resp.rating());
        }

        @Test @DisplayName("Session not found")
        void notFound() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new CreateReviewRequest(1L, 5, "x")));
        }

        @Test @DisplayName("Not completed")
        void notCompleted() {
            completedSession.setStatus(SessionStatus.ACCEPTED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new CreateReviewRequest(1L, 5, "x")));
        }

        @Test @DisplayName("Not learner")
        void notLearner() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            assertThrows(RuntimeException.class, () -> service.submitReview(999L, new CreateReviewRequest(1L, 5, "x")));
        }

        @Test @DisplayName("Already reviewed")
        void alreadyReviewed() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(true);
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new CreateReviewRequest(1L, 5, "x")));
        }

        @Test @DisplayName("RabbitMQ failure swallowed")
        void rabbitFailure() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.save(any())).thenReturn(testReview);
            when(reviewRepository.calculateAverageRating(10L)).thenReturn(null);
            when(reviewRepository.countByMentorId(10L)).thenReturn(1L);
            doThrow(new RuntimeException("down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
            assertDoesNotThrow(() -> service.submitReview(20L, new CreateReviewRequest(1L, 5, "x")));
        }
    }

    @Test @DisplayName("getMentorReviews")
    void getMentorReviews() {
        Page<Review> page = new PageImpl<>(List.of(testReview));
        when(reviewRepository.findByMentorIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getMentorReviews(10L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getMyReviews")
    void getMyReviews() {
        Page<Review> page = new PageImpl<>(List.of(testReview));
        when(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getMyReviews(20L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getReviewById - found")
    void getReviewById() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
        assertNotNull(service.getReviewById(100L));
    }

    @Test @DisplayName("getReviewById - not found")
    void getReviewById_notFound() {
        when(reviewRepository.findById(100L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> service.getReviewById(100L));
    }

    @Test @DisplayName("deleteReview")
    void deleteReview() {
        service.deleteReview(100L);
        verify(reviewRepository).deleteById(100L);
    }

    @Nested @DisplayName("getMentorRatingSummary")
    class RatingSummaryTests {
        @Test @DisplayName("With sessions")
        void withSessions() {
            when(reviewRepository.countByMentorId(10L)).thenReturn(5L);
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(10L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(2L);
            when(reviewRepository.calculateTotalRating(10L)).thenReturn(20.0);
            when(reviewRepository.getRatingDistribution(10L)).thenReturn(
                    List.of(new Object[]{5, 3L}, new Object[]{4, 2L}));

            MentorRatingSummary summary = service.getMentorRatingSummary(10L);
            assertFalse(summary.newMentor());
            assertEquals(2, summary.ratingDistribution().size());
        }

        @Test @DisplayName("No sessions - new mentor")
        void noSessions() {
            when(reviewRepository.countByMentorId(10L)).thenReturn(0L);
            when(sessionRepository.countByMentorIdAndStatus(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(sessionRepository.countByMentorIdAndStatusAndDefaultRatingAppliedTrue(10L, SessionStatus.COMPLETED)).thenReturn(0L);
            when(reviewRepository.calculateTotalRating(10L)).thenReturn(0.0);
            when(reviewRepository.getRatingDistribution(10L)).thenReturn(List.of());

            MentorRatingSummary summary = service.getMentorRatingSummary(10L);
            assertTrue(summary.newMentor());
            assertEquals(0.0, summary.averageRating());
        }
    }
}
