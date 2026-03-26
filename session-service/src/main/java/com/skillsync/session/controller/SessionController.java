package com.skillsync.session.controller;

import com.skillsync.session.dto.*;
import com.skillsync.session.service.command.SessionCommandService;
import com.skillsync.session.service.query.SessionQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionCommandService sessionCommandService;
    private final SessionQueryService sessionQueryService;

    // ─── QUERIES ───

    @GetMapping("/{id}")
    public ResponseEntity<SessionResponse> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionQueryService.getSessionById(id));
    }

    @GetMapping("/learner")
    public ResponseEntity<Page<SessionResponse>> getLearnerSessions(
            @RequestHeader("X-User-Id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(sessionQueryService.getSessionsByLearner(userId, pageable));
    }

    @GetMapping("/mentor")
    public ResponseEntity<Page<SessionResponse>> getMentorSessions(
            @RequestHeader("X-User-Id") Long userId, Pageable pageable) {
        return ResponseEntity.ok(sessionQueryService.getSessionsByMentor(userId, pageable));
    }

    // ─── COMMANDS ───

    @PostMapping
    public ResponseEntity<SessionResponse> createSession(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateSessionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sessionCommandService.createSession(userId, request));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<SessionResponse> acceptSession(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(sessionCommandService.acceptSession(id, userId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<SessionResponse> rejectSession(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(sessionCommandService.rejectSession(id, userId, reason));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<SessionResponse> cancelSession(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(sessionCommandService.cancelSession(id, userId));
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<SessionResponse> completeSession(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(sessionCommandService.completeSession(id, userId));
    }
}
