package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.command.UserCommandService;
import com.skillsync.user.service.query.UserQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    // ─── QUERIES ───

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userQueryService.getProfile(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(userQueryService.getProfileById(id));
    }

    // ─── COMMANDS ───

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userCommandService.createOrUpdateProfile(userId, request));
    }

    @PostMapping("/me/skills")
    public ResponseEntity<Void> addSkill(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddSkillRequest request) {
        userCommandService.addSkill(userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/skills/{skillId}")
    public ResponseEntity<Void> removeSkill(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long skillId) {
        userCommandService.removeSkill(userId, skillId);
        return ResponseEntity.ok().build();
    }
}
