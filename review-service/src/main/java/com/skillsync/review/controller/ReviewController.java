package com.skillsync.review.controller;

import com.skillsync.review.dto.*;
import com.skillsync.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/reviews") @RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.submitReview(userId, request));
    }

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<Page<ReviewResponse>> getMentorReviews(@PathVariable Long mentorId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMentorReviews(mentorId, pageable));
    }

    @GetMapping("/mentor/{mentorId}/summary")
    public ResponseEntity<MentorRatingSummary> getMentorRating(@PathVariable Long mentorId) {
        return ResponseEntity.ok(reviewService.getMentorRatingSummary(mentorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewService.getReviewById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(@RequestHeader("X-User-Id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(reviewService.getMyReviews(userId, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id); return ResponseEntity.ok().build();
    }
}
