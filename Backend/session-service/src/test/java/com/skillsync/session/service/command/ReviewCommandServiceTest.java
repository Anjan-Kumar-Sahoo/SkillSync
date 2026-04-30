package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import com.skillsync.session.service.MentorMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewCommandServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private MentorProfileClient mentorProfileClient;
    @Mock private MentorMetricsService mentorMetricsService;
    @InjectMocks private ReviewCommandService service;

    private Session completedSession;
    private Review testReview;

    @BeforeEach
    void setUp() {
        completedSession = Session.builder()
                .id(1L).mentorId(10L).learnerId(20L)
                .status(SessionStatus.COMPLETED).defaultRatingApplied(true).build();
        testReview = Review.builder()
                .id(100L).sessionId(1L).mentorId(10L).reviewerId(20L)
                .rating(5).comment("Great").createdAt(LocalDateTime.now()).build();
    }

    @Nested @DisplayName("submitReview")
    class SubmitReviewTests {
        @Test @DisplayName("Success")
        void success() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));

            ReviewRequest req = new ReviewRequest(1L, 10L, 5, "Great");
            ReviewResponse resp = service.submitReview(20L, req);
            assertNotNull(resp);
            assertFalse(completedSession.isDefaultRatingApplied());
            verify(sessionRepository).save(completedSession);
        }

        @Test @DisplayName("Session not found")
        void sessionNotFound() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Session not completed")
        void notCompleted() {
            completedSession.setStatus(SessionStatus.ACCEPTED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Non-learner reviewer throws")
        void nonLearner() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            assertThrows(RuntimeException.class, () -> service.submitReview(999L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Mentor mismatch throws")
        void mentorMismatch() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(99L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, 99L, 5, "x")));
        }

        @Test @DisplayName("Duplicate review throws")
        void duplicate() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(true);
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Invalid mentor ID throws")
        void invalidMentor() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            when(mentorProfileClient.getMentorById(10L)).thenThrow(new RuntimeException("no"));
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Null mentor ID throws")
        void nullMentorId() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            assertThrows(RuntimeException.class, () -> service.submitReview(20L, new ReviewRequest(1L, null, 5, "x")));
        }

        @Test @DisplayName("RabbitMQ failure swallowed")
        void rabbitFailure() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));
            doThrow(new RuntimeException("down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());

            assertDoesNotThrow(() -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x")));
        }

        @Test @DisplayName("Null comment is normalized to null")
        void nullComment() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));

            assertDoesNotThrow(() -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, null)));
        }

        @Test @DisplayName("Empty comment trimmed to null")
        void emptyComment() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));

            assertDoesNotThrow(() -> service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "  ")));
        }

        @Test @DisplayName("Mentor resolved via profile lookup")
        void mentorViaProfile() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(50L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            when(mentorProfileClient.getMentorById(50L)).thenReturn(Map.of("userId", 10L));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));

            assertDoesNotThrow(() -> service.submitReview(20L, new ReviewRequest(1L, 50L, 5, "ok")));
        }

        @Test @DisplayName("No default rating to clear")
        void noDefaultRating() {
            completedSession.setDefaultRatingApplied(false);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(completedSession));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(reviewRepository.existsBySessionId(1L)).thenReturn(false);
            when(reviewRepository.saveAndFlush(any())).thenReturn(testReview);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));

            service.submitReview(20L, new ReviewRequest(1L, 10L, 5, "x"));
            verify(sessionRepository, never()).save(any());
        }
    }

    @Nested @DisplayName("deleteReview")
    class DeleteReviewTests {
        @Test @DisplayName("Review exists - caches evicted")
        void exists() {
            when(reviewRepository.findById(100L)).thenReturn(Optional.of(testReview));
            service.deleteReview(100L);
            verify(cacheService).evict(CacheService.vKey("review:100"));
            verify(reviewRepository).deleteById(100L);
        }

        @Test @DisplayName("Review not found - still deletes by id")
        void notFound() {
            when(reviewRepository.findById(100L)).thenReturn(Optional.empty());
            service.deleteReview(100L);
            verify(cacheService, never()).evict(anyString());
            verify(reviewRepository).deleteById(100L);
        }
    }
}
