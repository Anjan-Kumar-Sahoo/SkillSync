package com.skillsync.session.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class ReviewSubmittedEvent {
    private Long reviewId;
    private Long mentorId;
    private int rating;
    private double newAvgRating;
    private int totalReviews;
}
