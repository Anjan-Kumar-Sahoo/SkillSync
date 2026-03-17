package com.skillsync.group.controller;

import com.skillsync.group.dto.*;
import com.skillsync.group.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/groups") @RequiredArgsConstructor
public class GroupController {
    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<GroupResponse> createGroup(@RequestHeader("X-User-Id") Long userId, @Valid @RequestBody CreateGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(userId, request));
    }

    @GetMapping
    public ResponseEntity<Page<GroupResponse>> getAllGroups(Pageable pageable) { return ResponseEntity.ok(groupService.getAllGroups(pageable)); }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> getGroup(@PathVariable Long id) { return ResponseEntity.ok(groupService.getGroupById(id)); }

    @PostMapping("/{id}/join")
    public ResponseEntity<Void> joinGroup(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        groupService.joinGroup(id, userId); return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId) {
        groupService.leaveGroup(id, userId); return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/discussions")
    public ResponseEntity<DiscussionResponse> postDiscussion(@PathVariable Long id, @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PostDiscussionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.postDiscussion(id, userId, request));
    }

    @GetMapping("/{id}/discussions")
    public ResponseEntity<Page<DiscussionResponse>> getDiscussions(@PathVariable Long id, Pageable pageable) {
        return ResponseEntity.ok(groupService.getDiscussions(id, pageable));
    }
}
