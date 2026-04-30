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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
    @InjectMocks private ReviewQueryService service;

    @Test @DisplayName("getReviewById - cache miss")
    void getReviewById_cacheMiss() {
        Review review = Review.builder().id(1L).sessionId(1L).mentorId(10L)
                .reviewerId(20L).rating(5).comment("Great").createdAt(LocalDateTime.now()).build();
        when(cacheService.getOrLoad(anyString(), eq(ReviewResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ReviewResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(review));
        assertNotNull(service.getReviewById(1L));
    }

    @Test @DisplayName("getReviewById - not found returns null")
    void getReviewById_notFound() {
        when(cacheService.getOrLoad(anyString(), eq(ReviewResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ReviewResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(reviewRepository.findById(1L)).thenReturn(Optional.empty());
        assertNull(service.getReviewById(1L));
    }

    @Test @DisplayName("getMentorReviews")
    void getMentorReviews() {
        Review review = Review.builder().id(1L).sessionId(1L).mentorId(10L)
                .reviewerId(20L).rating(5).comment("ok").createdAt(LocalDateTime.now()).build();
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByMentorIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getMentorReviews(10L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getMyReviews")
    void getMyReviews() {
        Review review = Review.builder().id(1L).sessionId(1L).mentorId(10L)
                .reviewerId(20L).rating(5).comment("ok").createdAt(LocalDateTime.now()).build();
        Page<Review> page = new PageImpl<>(List.of(review));
        when(reviewRepository.findByReviewerIdOrderByCreatedAtDesc(20L, PageRequest.of(0, 10))).thenReturn(page);
        assertEquals(1, service.getMyReviews(20L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test @DisplayName("getMentorRatingSummary - cached")
    void getMentorRatingSummary() {
        MentorRatingSummary summary = new MentorRatingSummary(10L, 4.5, 3, 5, 0, false, Map.of());
        when(cacheService.getOrLoad(anyString(), eq(MentorRatingSummary.class), any(), any()))
                .thenReturn(summary);
        assertEquals(summary, service.getMentorRatingSummary(10L));
    }
}
