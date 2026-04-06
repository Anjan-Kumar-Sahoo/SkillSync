package com.skillsync.user.controller;

import com.skillsync.user.dto.AdminStatsResponse;
import com.skillsync.user.dto.MentorProfileResponse;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.feign.SessionServiceClient;
import com.skillsync.user.repository.MentorProfileRepository;
import com.skillsync.user.service.command.MentorCommandService;
import com.skillsync.user.service.query.MentorQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final AuthServiceClient authServiceClient;
    private final SessionServiceClient sessionServiceClient;
    private final MentorProfileRepository mentorProfileRepository;
    private final MentorQueryService mentorQueryService;
    private final MentorCommandService mentorCommandService;

    // ─── STATS ───

    @GetMapping("/stats")
    public ResponseEntity<AdminStatsResponse> getStats() {
        long totalUsers = 0;
        long totalSessions = 0;

        try {
            totalUsers = authServiceClient.getUserCount(null);
        } catch (Exception e) {
            log.warn("Failed to fetch user count from auth-service: {}", e.getMessage());
        }

        try {
            totalSessions = sessionServiceClient.getSessionCount();
        } catch (Exception e) {
            log.warn("Failed to fetch session count from session-service: {}", e.getMessage());
        }

        long totalMentors = mentorProfileRepository.countByStatus(MentorStatus.APPROVED);
        long pendingApprovals = mentorProfileRepository.countByStatus(MentorStatus.PENDING);

        return ResponseEntity.ok(new AdminStatsResponse(
                totalUsers, totalMentors, totalSessions, pendingApprovals));
    }

    // ─── USERS ───

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        try {
            Map<String, Object> users = authServiceClient.getAllUsers(page, size);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Failed to fetch users from auth-service: {}", e.getMessage());
            return ResponseEntity.ok(Map.of("content", java.util.List.of(), "totalElements", 0));
        }
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id, @RequestBody Map<String, String> body) {
        String role = body.get("role");
        authServiceClient.updateUserRole(id, role);
        return ResponseEntity.ok().build();
    }

    // ─── MENTORS ───

    @GetMapping("/mentors/pending")
    public ResponseEntity<Page<MentorProfileResponse>> getPendingMentors(Pageable pageable) {
        return ResponseEntity.ok(mentorQueryService.getPendingApplications(pageable));
    }

    @PostMapping("/mentors/{id}/approve")
    public ResponseEntity<Void> approveMentor(@PathVariable Long id) {
        mentorCommandService.approveMentor(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mentors/{id}/reject")
    public ResponseEntity<Void> rejectMentor(
            @PathVariable Long id,
            @RequestParam(defaultValue = "Rejected by admin") String reason) {
        mentorCommandService.rejectMentor(id, reason);
        return ResponseEntity.ok().build();
    }
}
