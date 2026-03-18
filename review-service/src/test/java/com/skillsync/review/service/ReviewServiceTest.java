package com.skillsync.review.service;

import com.skillsync.review.dto.CreateReviewRequest;
import com.skillsync.review.dto.ReviewResponse;
import com.skillsync.review.entity.Review;
import com.skillsync.review.feign.SessionServiceClient;
import com.skillsync.review.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private SessionServiceClient sessionServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private ReviewService reviewService;

    private Review testReview;

    @BeforeEach
    void setUp() {
        testReview = Review.builder()
                .id(1L)
                .sessionId(10L)
                .mentorId(2L)
                .reviewerId(3L)
                .rating(5)
                .comment("Great session!")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Submit review - success")
    void submitReview_shouldCreateReview() {
        CreateReviewRequest request = new CreateReviewRequest(10L, 5, "Great session!");
        Map<String, Object> session = Map.of("status", "COMPLETED", "learnerId", 3L, "mentorId", 2L);

        when(sessionServiceClient.getSessionById(10L)).thenReturn(session);
        when(reviewRepository.existsBySessionId(10L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);
        when(reviewRepository.calculateAverageRating(2L)).thenReturn(5.0);
        when(reviewRepository.countByMentorId(2L)).thenReturn(1L);

        ReviewResponse response = reviewService.submitReview(3L, request);

        assertNotNull(response);
        assertEquals(5, response.rating());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("Submit review - non-completed session throws exception")
    void submitReview_shouldThrowForNonCompletedSession() {
        CreateReviewRequest request = new CreateReviewRequest(10L, 5, "Good");
        Map<String, Object> session = Map.of("status", "REQUESTED", "learnerId", 3L, "mentorId", 2L);
        when(sessionServiceClient.getSessionById(10L)).thenReturn(session);

        assertThrows(RuntimeException.class, () -> reviewService.submitReview(3L, request));
    }

    @Test
    @DisplayName("Get review by ID - success")
    void getReviewById_shouldReturnReview() {
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        ReviewResponse response = reviewService.getReviewById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Get review by ID - not found throws exception")
    void getReviewById_shouldThrowWhenNotFound() {
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> reviewService.getReviewById(999L));
    }

    @Test
    @DisplayName("Delete review - calls repository")
    void deleteReview_shouldCallRepository() {
        reviewService.deleteReview(1L);
        verify(reviewRepository).deleteById(1L);
    }
}
