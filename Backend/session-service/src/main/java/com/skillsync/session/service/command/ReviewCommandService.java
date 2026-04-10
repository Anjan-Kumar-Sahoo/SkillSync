package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Review;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.event.ReviewSubmittedEvent;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.mapper.ReviewMapper;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
    private final AuthServiceClient authServiceClient;
    private final MentorProfileClient mentorProfileClient;

    @Transactional
    public ReviewResponse submitReview(Long reviewerId, ReviewRequest request) {
        Long mentorId = resolveMentorUserId(request.mentorId());
        Map<String, Object> mentor = fetchUserById(mentorId);
        if (!isMentorUser(mentor)) {
            throw new RuntimeException("Invalid mentor id: " + request.mentorId());
        }

        if (reviewRepository.existsByMentorIdAndReviewerId(mentorId, reviewerId)) {
            throw new RuntimeException("You have already reviewed this mentor");
        }

        Session session = sessionRepository
                .findFirstByMentorIdAndLearnerIdAndStatusOrderBySessionDateDesc(
                        mentorId,
                        reviewerId,
                        SessionStatus.COMPLETED
                )
                .orElseThrow(() -> new RuntimeException("No completed session found with this mentor"));

        Review review = Review.builder()
                .sessionId(session.getId()).mentorId(mentorId).reviewerId(reviewerId)
                .rating(request.rating()).comment(normalizeComment(request.comment())).build();
        review = reviewRepository.saveAndFlush(review);

        // Invalidate versioned review caches
        cacheService.evictByPattern(CacheService.vKey("review:mentor:" + mentorId + ":*"));
        cacheService.evictByPattern(CacheService.vKey("review:user:" + reviewerId + ":*"));

        // Publish event
        Double avgRating = reviewRepository.calculateAverageRating(mentorId);
        long totalReviews = reviewRepository.countByMentorId(mentorId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.REVIEW_EXCHANGE, "review.submitted",
                    new ReviewSubmittedEvent(review.getId(), mentorId, review.getRating(),
                            avgRating != null ? avgRating : 0.0, (int) totalReviews,
                            review.getComment()));
        } catch (Exception e) {
            log.error("Failed to publish review event: {}", e.getMessage());
        }

        log.info("[CQRS:COMMAND] Review {} submitted. Cache invalidated.", review.getId());
        return ReviewMapper.toResponse(review);
    }

    private String normalizeComment(String comment) {
        if (comment == null) {
            return null;
        }
        String normalized = comment.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private Long resolveMentorUserId(Long requestedMentorId) {
        if (requestedMentorId == null) {
            throw new RuntimeException("Mentor ID is required");
        }

        Map<String, Object> user = fetchUserById(requestedMentorId);
        if (isMentorUser(user)) {
            return requestedMentorId;
        }

        Long mappedUserId = mapMentorProfileIdToUserId(requestedMentorId);
        if (mappedUserId != null) {
            Map<String, Object> mappedUser = fetchUserById(mappedUserId);
            if (isMentorUser(mappedUser)) {
                return mappedUserId;
            }
        }

        throw new RuntimeException("Invalid mentor id: " + requestedMentorId);
    }

    private Map<String, Object> fetchUserById(Long userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Unable to validate user {} via auth-service: {}", userId, e.getMessage());
            return null;
        }
    }

    private boolean isMentorUser(Map<String, Object> userPayload) {
        if (userPayload == null) {
            return false;
        }
        Object role = userPayload.get("role");
        return "ROLE_MENTOR".equals(String.valueOf(role));
    }

    private Long mapMentorProfileIdToUserId(Long mentorProfileId) {
        try {
            Map<String, Object> mentorProfile = mentorProfileClient.getMentorById(mentorProfileId);
            Object userIdValue = mentorProfile.get("userId");
            if (userIdValue instanceof Number number) {
                return number.longValue();
            }
            if (userIdValue != null) {
                return Long.parseLong(String.valueOf(userIdValue));
            }
        } catch (Exception e) {
            log.debug("Mentor profile lookup failed for id {}: {}", mentorProfileId, e.getMessage());
        }
        return null;
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
