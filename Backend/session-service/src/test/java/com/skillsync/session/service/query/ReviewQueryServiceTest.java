package com.skillsync.session.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.service.MentorMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewQueryServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private MentorMetricsService mentorMetricsService;
    @Mock private CacheService cacheService;

    @InjectMocks private ReviewQueryService reviewQueryService;

    private Review buildTestReview() {
        return Review.builder()
                .id(1L).sessionId(10L).mentorId(100L).reviewerId(200L)
                .rating(5).comment("Great!").createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("getReviewById — cache miss loads from DB")
    void getReviewById_shouldLoadFromDb() {
        Review review = buildTestReview();
        when(cacheService.getOrLoad(anyString(), eq(ReviewResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ReviewResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));

        ReviewResponse result = reviewQueryService.getReviewById(1L);

        assertNotNull(result);
        assertEquals(5, result.rating());
    }

    @Test
    @DisplayName("getReviewById — returns null for non-existent review")
    void getReviewById_shouldReturnNullForMissing() {
        when(cacheService.getOrLoad(anyString(), eq(ReviewResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ReviewResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(reviewRepository.findById(999L)).thenReturn(Optional.empty());

        ReviewResponse result = reviewQueryService.getReviewById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("getMentorReviews — returns paginated reviews")
    void getMentorReviews_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Review review = buildTestReview();
        when(reviewRepository.findByMentorIdOrderByCreatedAtDesc(100L, pageable))
                .thenReturn(new PageImpl<>(List.of(review)));

        var result = reviewQueryService.getMentorReviews(100L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(5, result.getContent().get(0).rating());
    }

    @Test
    @DisplayName("getMyReviews — returns paginated reviews for reviewer")
    void getMyReviews_shouldReturnPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Review review = buildTestReview();
        when(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(200L, pageable))
                .thenReturn(new PageImpl<>(List.of(review)));

        var result = reviewQueryService.getMyReviews(200L, pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("getMentorRatingSummary — cache miss loads from metrics service")
    void getMentorRatingSummary_shouldLoadFromMetrics() {
        MentorRatingSummary summary = new MentorRatingSummary(
                100L, 4.5, 10, 20L, 2L, false, Map.of(5, 7, 4, 3));
        when(cacheService.getOrLoad(anyString(), eq(MentorRatingSummary.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorRatingSummary> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorMetricsService.calculateMentorRatingSummary(100L)).thenReturn(summary);

        MentorRatingSummary result = reviewQueryService.getMentorRatingSummary(100L);

        assertNotNull(result);
        assertEquals(4.5, result.averageRating());
    }
}
