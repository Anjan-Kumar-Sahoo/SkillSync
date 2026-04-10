package com.skillsync.session.controller;

import com.skillsync.session.dto.*;
import com.skillsync.session.service.command.ReviewCommandService;
import com.skillsync.session.service.query.ReviewQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    // ─── QUERIES ───

    @GetMapping("/mentor/{mentorId}")
    public ResponseEntity<Page<ReviewResponse>> getMentorReviews(
            @PathVariable Long mentorId, Pageable pageable) {
        return ResponseEntity.ok(reviewQueryService.getMentorReviews(mentorId, pageable));
    }

    @GetMapping("/mentor/{mentorId}/summary")
    public ResponseEntity<MentorRatingSummary> getMentorRating(@PathVariable Long mentorId) {
        return ResponseEntity.ok(reviewQueryService.getMentorRatingSummary(mentorId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReviewResponse> getReview(@PathVariable Long id) {
        return ResponseEntity.ok(reviewQueryService.getReviewById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<Page<ReviewResponse>> getMyReviews(
            @RequestHeader("X-User-Id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(reviewQueryService.getMyReviews(userId, pageable));
    }

    // ─── COMMANDS ───

    @PostMapping
    public ResponseEntity<ReviewResponse> submitReview(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewCommandService.submitReview(userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long id) {
        reviewCommandService.deleteReview(id);
        return ResponseEntity.ok().build();
    }
}
