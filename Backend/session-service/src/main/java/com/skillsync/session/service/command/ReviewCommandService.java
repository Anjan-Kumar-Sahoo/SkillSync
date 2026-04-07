package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.event.ReviewSubmittedEvent;
import com.skillsync.session.mapper.ReviewMapper;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS Command Service for Review operations.
 * Handles all write operations, cache invalidation, and event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;

    @Transactional
    public ReviewResponse submitReview(Long reviewerId, CreateReviewRequest request) {
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new RuntimeException("Session not found: " + request.sessionId()));

        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new RuntimeException("Can only review completed sessions");
        }
        if (!reviewerId.equals(session.getLearnerId())) {
            throw new RuntimeException("Only the learner can review");
        }
        if (reviewRepository.existsBySessionId(request.sessionId())) {
            throw new RuntimeException("Session already reviewed");
        }

        Long mentorId = session.getMentorId();

        Review review = Review.builder()
                .sessionId(request.sessionId()).mentorId(mentorId).reviewerId(reviewerId)
                .rating(request.rating()).comment(request.comment()).build();
        review = reviewRepository.saveAndFlush(review);

        // Invalidate versioned review caches
        cacheService.evictByPattern(CacheService.vKey("review:mentor:" + mentorId + ":*"));
        cacheService.evictByPattern(CacheService.vKey("review:user:" + reviewerId + ":*"));

        // Publish event
        Double avgRating = reviewRepository.calculateAverageRating(mentorId);
        long totalReviews = reviewRepository.countByMentorId(mentorId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_EXCHANGE, "review.submitted",
                    new ReviewSubmittedEvent(review.getId(), mentorId, request.rating(),
                            avgRating != null ? avgRating : 0.0, (int) totalReviews));
        } catch (Exception e) {
            log.error("Failed to publish review event: {}", e.getMessage());
        }

        log.info("[CQRS:COMMAND] Review {} submitted. Cache invalidated.", review.getId());
        return ReviewMapper.toResponse(review);
    }

    public void deleteReview(Long id) {
        Review review = reviewRepository.findById(id).orElse(null);
        if (review != null) {
            cacheService.evict(CacheService.vKey("review:" + id));
            cacheService.evictByPattern(CacheService.vKey("review:mentor:" + review.getMentorId() + ":*"));
            cacheService.evictByPattern(CacheService.vKey("review:user:" + review.getReviewerId() + ":*"));
        }
        reviewRepository.deleteById(id);
        log.info("[CQRS:COMMAND] Review {} deleted. Cache invalidated.", id);
    }
}
