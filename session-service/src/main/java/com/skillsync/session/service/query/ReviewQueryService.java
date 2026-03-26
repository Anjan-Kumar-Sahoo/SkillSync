package com.skillsync.session.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CQRS Query Service for Review operations.
 * Cache-aside with stampede + penetration protection (5-minute TTL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final CacheService cacheService;

    @Value("${cache.ttl.review:300}")
    private long reviewTtl;

    /**
     * Cache-aside with stampede protection: get review by ID.
     */
    public ReviewResponse getReviewById(Long id) {
        String cacheKey = CacheService.vKey("review:" + id);

        return cacheService.getOrLoad(cacheKey, ReviewResponse.class,
                Duration.ofSeconds(reviewTtl), () -> {
                    Review review = reviewRepository.findById(id).orElse(null);
                    if (review == null) return null;
                    return mapToResponse(review);
                });
    }

    public Page<ReviewResponse> getMentorReviews(Long mentorId, Pageable pageable) {
        return reviewRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, pageable)
                .map(ReviewQueryService::mapToResponse);
    }

    public Page<ReviewResponse> getMyReviews(Long reviewerId, Pageable pageable) {
        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId, pageable)
                .map(ReviewQueryService::mapToResponse);
    }

    /**
     * Cache-aside with stampede protection for mentor rating summary.
     */
    public MentorRatingSummary getMentorRatingSummary(Long mentorId) {
        String cacheKey = CacheService.vKey("review:mentor:" + mentorId + ":summary");

        return cacheService.getOrLoad(cacheKey, MentorRatingSummary.class,
                Duration.ofSeconds(reviewTtl), () -> {
                    Double avg = reviewRepository.calculateAverageRating(mentorId);
                    long total = reviewRepository.countByMentorId(mentorId);
                    List<Object[]> distribution = reviewRepository.getRatingDistribution(mentorId);
                    Map<Integer, Integer> distMap = new HashMap<>();
                    for (Object[] row : distribution) {
                        distMap.put((Integer) row[0], ((Long) row[1]).intValue());
                    }
                    return new MentorRatingSummary(
                            mentorId, avg != null ? avg : 0.0, (int) total, distMap);
                });
    }

    /**
     * Shared mapper — also used by ReviewCommandService.
     */
    public static ReviewResponse mapToResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getSessionId(), r.getMentorId(), r.getReviewerId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
