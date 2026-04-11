package com.skillsync.session.service;

import com.skillsync.session.dto.MentorMetricsResponse;
import com.skillsync.session.dto.MentorRatingSummary;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.ReviewRepository;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MentorMetricsService {

    private static final double DEFAULT_RATING = 2.5d;

    private final SessionRepository sessionRepository;
    private final ReviewRepository reviewRepository;

    public MentorMetricsResponse calculateMentorMetrics(Long mentorId) {
        long completedSessions = sessionRepository.countByMentorIdAndStatus(mentorId, SessionStatus.COMPLETED);
        long defaultRatedSessions = sessionRepository
                .countByMentorIdAndStatusAndDefaultRatingAppliedTrue(mentorId, SessionStatus.COMPLETED);
        long totalReviewsLong = reviewRepository.countByMentorId(mentorId);
        int totalReviews = Math.toIntExact(totalReviewsLong);

        double averageRating = 0.0d;
        if (completedSessions > 0) {
            double totalExplicitRating = reviewRepository.calculateTotalRating(mentorId);
            double computed = (totalExplicitRating + (defaultRatedSessions * DEFAULT_RATING)) / completedSessions;
            averageRating = BigDecimal.valueOf(computed)
                    .setScale(2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new MentorMetricsResponse(
                mentorId,
                completedSessions,
                averageRating,
                totalReviews,
                defaultRatedSessions,
                completedSessions == 0
        );
    }

    public MentorRatingSummary calculateMentorRatingSummary(Long mentorId) {
        MentorMetricsResponse metrics = calculateMentorMetrics(mentorId);
        List<Object[]> distribution = reviewRepository.getRatingDistribution(mentorId);
        Map<Integer, Integer> distMap = new HashMap<>();
        for (Object[] row : distribution) {
            distMap.put((Integer) row[0], ((Long) row[1]).intValue());
        }

        return new MentorRatingSummary(
                metrics.mentorId(),
                metrics.averageRating(),
                metrics.totalReviews(),
                metrics.completedSessions(),
                metrics.defaultRatedSessions(),
                metrics.newMentor(),
                distMap
        );
    }
}
