package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateMyProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.createOrUpdateProfile(userId, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getProfileById(id));
    }

    @PostMapping("/me/skills")
    public ResponseEntity<Void> addSkill(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AddSkillRequest request) {
        userService.addSkill(userId, request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/me/skills/{skillId}")
    public ResponseEntity<Void> removeSkill(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long skillId) {
        userService.removeSkill(userId, skillId);
        return ResponseEntity.ok().build();
    }
}
