package com.skillsync.session.dto;

import java.util.Map;

public record MentorRatingSummary(
	Long mentorId,
	double averageRating,
	int totalReviews,
	long totalSessions,
	long defaultRatedSessions,
	boolean newMentor,
	Map<Integer, Integer> ratingDistribution
) {}
