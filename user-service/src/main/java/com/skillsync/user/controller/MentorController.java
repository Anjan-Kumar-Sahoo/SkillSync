package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.MentorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mentors")
@RequiredArgsConstructor
public class MentorController {

    private final MentorService mentorService;

    @PostMapping("/apply")
    public ResponseEntity<MentorProfileResponse> apply(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MentorApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mentorService.apply(userId, request));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<MentorProfileResponse>> searchMentors(Pageable pageable) {
        return ResponseEntity.ok(mentorService.searchMentors(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MentorProfileResponse> getMentorById(@PathVariable Long id) {
        return ResponseEntity.ok(mentorService.getMentorById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<MentorProfileResponse> getMyMentorProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(mentorService.getMentorByUserId(userId));
    }

    @PostMapping("/me/availability")
    public ResponseEntity<AvailabilitySlotResponse> addAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.ok(mentorService.addAvailability(userId, request));
    }

    @DeleteMapping("/me/availability/{id}")
    public ResponseEntity<Void> removeAvailability(@PathVariable Long id) {
        mentorService.removeAvailability(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveMentor(@PathVariable Long id) {
        mentorService.approveMentor(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectMentor(@PathVariable Long id, @RequestParam String reason) {
        mentorService.rejectMentor(id, reason);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<MentorProfileResponse>> getPendingApplications(Pageable pageable) {
        return ResponseEntity.ok(mentorService.getPendingApplications(pageable));
    }
}
