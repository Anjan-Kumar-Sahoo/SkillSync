package com.skillsync.session.review.dto;

import java.util.Map;

public record MentorRatingSummary(Long mentorId, double averageRating, int totalReviews, Map<Integer, Integer> ratingDistribution) {}
