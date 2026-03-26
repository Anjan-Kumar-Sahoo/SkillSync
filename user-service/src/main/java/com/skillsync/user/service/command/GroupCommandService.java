package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.repository.*;
import com.skillsync.user.service.query.GroupQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * CQRS Command Service for Group operations.
 * Handles all write operations and cache invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCommandService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final DiscussionRepository discussionRepository;
    private final CacheService cacheService;

    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        LearningGroup group = LearningGroup.builder()
                .name(request.name()).description(request.description())
                .maxMembers(request.maxMembers()).createdBy(userId)
                .members(new ArrayList<>()).build();
        group = groupRepository.save(group);

        GroupMember owner = GroupMember.builder().group(group).userId(userId)
                .role(GroupMember.MemberRole.OWNER).build();
        memberRepository.save(owner);

        cacheService.evictByPattern(CacheService.vKey("user:group:all:*"));
        log.info("[CQRS:COMMAND] Group created by userId: {}. Cache invalidated.", userId);
        return GroupQueryService.mapToResponse(group, 1);
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        LearningGroup group = findGroup(groupId);
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new RuntimeException("Already a member");
        long count = memberRepository.countByGroupId(groupId);
        if (count >= group.getMaxMembers()) throw new RuntimeException("Group is full");
        memberRepository.save(GroupMember.builder().group(group).userId(userId)
                .role(GroupMember.MemberRole.MEMBER).build());

        cacheService.evict(CacheService.vKey("user:group:" + groupId));
        log.info("[CQRS:COMMAND] User {} joined group {}. Cache invalidated.", userId, groupId);
    }

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Not a member of this group"));
        if (member.getRole() == GroupMember.MemberRole.OWNER)
            throw new RuntimeException("Owner cannot leave the group");
        memberRepository.delete(member);

        cacheService.evict(CacheService.vKey("user:group:" + groupId));
        log.info("[CQRS:COMMAND] User {} left group {}. Cache invalidated.", userId, groupId);
    }

    @Transactional
    public DiscussionResponse postDiscussion(Long groupId, Long userId, PostDiscussionRequest request) {
        LearningGroup group = findGroup(groupId);
        if (!memberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new RuntimeException("Must be a member to post");
        Discussion parent = request.parentId() != null
                ? discussionRepository.findById(request.parentId()).orElse(null) : null;
        Discussion discussion = Discussion.builder().group(group).authorId(userId)
                .content(request.content()).parent(parent).build();
        discussion = discussionRepository.save(discussion);

        cacheService.evictByPattern(CacheService.vKey("user:group:" + groupId + ":discussions:*"));
        return GroupQueryService.mapDiscussion(discussion);
    }

    private LearningGroup findGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
    }
}
