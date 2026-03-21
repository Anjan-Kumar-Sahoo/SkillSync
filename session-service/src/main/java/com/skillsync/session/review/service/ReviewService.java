package com.skillsync.session.review.service;

import com.skillsync.session.review.dto.*;
import com.skillsync.session.review.entity.Review;
import com.skillsync.session.review.event.ReviewSubmittedEvent;
import com.skillsync.session.review.repository.ReviewRepository;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.SessionRepository;
import com.skillsync.session.review.config.ReviewRabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service @RequiredArgsConstructor @Slf4j
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final SessionRepository sessionRepository;  // Direct local access — no Feign needed!
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public ReviewResponse submitReview(Long reviewerId, CreateReviewRequest request) {
        // Validate session — now a direct DB call instead of Feign
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
        review = reviewRepository.save(review);

        // Calculate new avg and publish event
        Double avgRating = reviewRepository.calculateAverageRating(mentorId);
        long totalReviews = reviewRepository.countByMentorId(mentorId);
        try {
            rabbitTemplate.convertAndSend(ReviewRabbitMQConfig.REVIEW_EXCHANGE, "review.submitted",
                    new ReviewSubmittedEvent(review.getId(), mentorId, request.rating(),
                            avgRating != null ? avgRating : 0.0, (int) totalReviews));
        } catch (Exception e) {
            log.error("Failed to publish review event: {}", e.getMessage());
        }

        log.info("Review {} submitted for session {} by reviewer {}", review.getId(), request.sessionId(), reviewerId);
        return mapToResponse(review);
    }

    public Page<ReviewResponse> getMentorReviews(Long mentorId, Pageable pageable) {
        return reviewRepository.findByMentorIdOrderByCreatedAtDesc(mentorId, pageable).map(this::mapToResponse);
    }

    public Page<ReviewResponse> getMyReviews(Long reviewerId, Pageable pageable) {
        return reviewRepository.findByReviewerIdOrderByCreatedAtDesc(reviewerId, pageable).map(this::mapToResponse);
    }

    public ReviewResponse getReviewById(Long id) {
        return reviewRepository.findById(id).map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Review not found: " + id));
    }

    public MentorRatingSummary getMentorRatingSummary(Long mentorId) {
        Double avg = reviewRepository.calculateAverageRating(mentorId);
        long total = reviewRepository.countByMentorId(mentorId);
        List<Object[]> distribution = reviewRepository.getRatingDistribution(mentorId);
        Map<Integer, Integer> distMap = new HashMap<>();
        for (Object[] row : distribution) {
            distMap.put((Integer) row[0], ((Long) row[1]).intValue());
        }
        return new MentorRatingSummary(mentorId, avg != null ? avg : 0.0, (int) total, distMap);
    }

    public void deleteReview(Long id) { reviewRepository.deleteById(id); }

    private ReviewResponse mapToResponse(Review r) {
        return new ReviewResponse(r.getId(), r.getSessionId(), r.getMentorId(), r.getReviewerId(),
                r.getRating(), r.getComment(), r.getCreatedAt());
    }
}
