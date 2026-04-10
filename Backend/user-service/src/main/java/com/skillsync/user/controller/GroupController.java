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
    public ResponseEntity<Page<GroupResponse>> getAllGroups(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getAllGroups(search, category, userId, pageable));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<GroupResponse>> getMyGroups(
            @RequestHeader("X-User-Id") Long userId,
            Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getMyGroups(userId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return ResponseEntity.ok(groupQueryService.getGroupById(id, userId));
    }

    @GetMapping("/{id}/members")
    public ResponseEntity<Page<GroupMemberResponse>> getMembers(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getGroupMembers(id, pageable));
    }

    @GetMapping("/{id}/discussions")
    public ResponseEntity<Page<DiscussionResponse>> getDiscussions(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            Pageable pageable) {
        return ResponseEntity.ok(groupQueryService.getDiscussions(id, userId, userRole, pageable));
    }

    // ─── COMMANDS ───

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupCommandService.createGroup(userId, userRole, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> updateGroup(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody UpdateGroupRequest request) {
        return ResponseEntity.ok(groupCommandService.updateGroup(id, userId, userRole, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        groupCommandService.deleteGroup(id, userId, userRole);
        return ResponseEntity.ok().build();
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

    @PostMapping("/{id}/members")
    public ResponseEntity<GroupMemberResponse> addMember(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole,
            @Valid @RequestBody AddGroupMemberRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(groupCommandService.addMember(id, userId, userRole, request));
    }

    @DeleteMapping("/{id}/members/{memberUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id,
            @PathVariable Long memberUserId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        groupCommandService.removeMember(id, memberUserId, userId, userRole);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/discussions")
    public ResponseEntity<DiscussionResponse> postDiscussion(
            @PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PostDiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupCommandService.postDiscussion(id, userId, request));
    }

    @DeleteMapping("/{id}/discussions/{discussionId}")
    public ResponseEntity<Void> deleteDiscussion(
            @PathVariable Long id,
            @PathVariable Long discussionId,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") String userRole) {
        groupCommandService.deleteDiscussion(id, discussionId, userId, userRole);
        return ResponseEntity.ok().build();
    }
}
