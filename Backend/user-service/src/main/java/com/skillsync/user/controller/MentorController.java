package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.command.MentorCommandService;
import com.skillsync.user.service.query.MentorQueryService;
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

    private final MentorCommandService mentorCommandService;
    private final MentorQueryService mentorQueryService;

    // ─── QUERIES ───

    @GetMapping("/search")
    public ResponseEntity<Page<MentorProfileResponse>> searchMentors(Pageable pageable) {
        return ResponseEntity.ok(mentorQueryService.searchMentors(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MentorProfileResponse> getMentorById(@PathVariable Long id) {
        return ResponseEntity.ok(mentorQueryService.getMentorById(id));
    }

    @GetMapping("/me")
    public ResponseEntity<MentorProfileResponse> getMyMentorProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(mentorQueryService.getMentorByUserId(userId));
    }

    @GetMapping("/pending")
    public ResponseEntity<Page<MentorProfileResponse>> getPendingApplications(Pageable pageable) {
        return ResponseEntity.ok(mentorQueryService.getPendingApplications(pageable));
    }

    @GetMapping("/me/availability")
    public ResponseEntity<?> getMyAvailability(@RequestHeader("X-User-Id") Long userId) {
        MentorProfileResponse profile = mentorQueryService.getMentorByUserId(userId);
        if (profile == null) {
            return ResponseEntity.ok(java.util.List.of());
        }
        return ResponseEntity.ok(profile.availability());
    }


    // ─── COMMANDS ───

    @PostMapping("/apply")
    public ResponseEntity<MentorProfileResponse> apply(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody MentorApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mentorCommandService.apply(userId, request));
    }

    @PostMapping("/me/availability")
    public ResponseEntity<AvailabilitySlotResponse> addAvailability(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AvailabilitySlotRequest request) {
        return ResponseEntity.ok(mentorCommandService.addAvailability(userId, request));
    }

    @DeleteMapping("/me/availability/{id}")
    public ResponseEntity<Void> removeAvailability(@PathVariable Long id) {
        mentorCommandService.removeAvailability(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveMentor(@PathVariable Long id) {
        mentorCommandService.approveMentor(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectMentor(@PathVariable Long id, @RequestParam String reason) {
        mentorCommandService.rejectMentor(id, reason);
        return ResponseEntity.ok().build();
    }
}
