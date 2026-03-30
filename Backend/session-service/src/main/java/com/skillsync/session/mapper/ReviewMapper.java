package com.skillsync.session.mapper;

import com.skillsync.session.dto.ReviewResponse;
import com.skillsync.session.entity.Review;

/**
 * Pure mapping functions for Review entities.
 * Used by both ReviewCommandService and ReviewQueryService (CQRS decoupling).
 */
public final class ReviewMapper {

    private ReviewMapper() {}

    public static ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(), review.getSessionId(), review.getMentorId(),
                review.getReviewerId(), review.getRating(), review.getComment(),
                review.getCreatedAt()
        );
    }
}
