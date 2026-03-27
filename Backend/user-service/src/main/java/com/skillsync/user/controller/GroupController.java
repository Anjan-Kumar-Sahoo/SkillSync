package com.skillsync.user.controller;

import com.skillsync.user.dto.*;
import com.skillsync.user.service.command.GroupCommandService;
import com.skillsync.user.service.query.GroupQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupCommandService groupCommandService;
    private final GroupQueryService groupQueryService;

    // ─── QUERIES ───

    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getAllGroups(Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getAllGroups(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long id) {
        return ResponseEntity.ok(groupQueryService.getGroupById(id));
    }

    @GetMapping("/{id}/discussions")
    public ResponseEntity<Page<DiscussionResponse>> getDiscussions(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getDiscussions(id, pageable));
    }

    // ─── COMMANDS ───

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupCommandService.createGroup(userId, request));
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinGroup(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        groupCommandService.joinGroup(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        groupCommandService.leaveGroup(id, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/discussions")
    public ResponseEntity<DiscussionResponse> postDiscussion(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PostDiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupCommandService.postDiscussion(id, userId, request));
    }
}
