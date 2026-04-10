package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.mapper.GroupMapper;
import com.skillsync.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CQRS Command Service for Group operations.
 * Handles all write operations and cache invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCommandService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_MENTOR = "ROLE_MENTOR";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final DiscussionRepository discussionRepository;
    private final AuthServiceClient authServiceClient;
    private final CacheService cacheService;

    @Transactional
    public GroupResponse createGroup(Long userId, String userRole, CreateGroupRequest request) {
        validateCanCreateGroup(userRole);

        Integer maxMembers = request.maxMembers() != null ? request.maxMembers() : Integer.MAX_VALUE;
        String normalizedName = request.name().trim();
        LearningGroup group = LearningGroup.builder()
            .name(normalizedName).description(normalizeDescription(request.description()))
            .category(normalizeCategory(request.category()))
            .maxMembers(maxMembers).createdBy(userId)
                .members(new ArrayList<>()).build();
        group = groupRepository.save(group);

        GroupMember owner = GroupMember.builder().group(group).userId(userId)
                .role(GroupMember.MemberRole.OWNER).build();
        memberRepository.save(owner);

        cacheService.evictByPattern(CacheService.vKey("user:group:all:*"));
        cacheService.evictByPattern(CacheService.vKey("user:group:my:*"));
        log.info("[CQRS:COMMAND] Group created by userId: {}. Cache invalidated.", userId);
        return GroupMapper.toResponse(group, 1, true);
    }

    @Transactional
    public GroupResponse updateGroup(Long groupId, Long actorUserId, String actorRole, UpdateGroupRequest request) {
        validateAdmin(actorRole);

        LearningGroup group = findGroup(groupId);
        if (request.name() != null && !request.name().isBlank()) {
            group.setName(request.name().trim());
        }
        if (request.description() != null) {
            group.setDescription(request.description().trim());
        }
        if (request.category() != null) {
            group.setCategory(normalizeCategory(request.category()));
        }
        if (request.maxMembers() != null) {
            group.setMaxMembers(request.maxMembers());
        }

        group = groupRepository.save(group);
        evictGroupCaches(groupId);

        int count = (int) memberRepository.countByGroupId(groupId);
        boolean joined = memberRepository.existsByGroupIdAndUserId(groupId, actorUserId);
        return GroupMapper.toResponse(group, count, joined);
    }

    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        LearningGroup group = findGroup(groupId);
        if (memberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new RuntimeException("Already a member");
        memberRepository.save(GroupMember.builder().group(group).userId(userId)
                .role(GroupMember.MemberRole.MEMBER).build());

        cacheService.evict(CacheService.vKey("user:group:" + groupId));
        cacheService.evictByPattern(CacheService.vKey("user:group:my:*"));
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
        cacheService.evictByPattern(CacheService.vKey("user:group:my:*"));
        log.info("[CQRS:COMMAND] User {} left group {}. Cache invalidated.", userId, groupId);
    }

    @Transactional
    public GroupMemberResponse addMember(Long groupId, Long actorUserId, String actorRole, AddGroupMemberRequest request) {
        LearningGroup group = findGroup(groupId);
        validateAdmin(actorRole);

        Map<String, Object> user = resolveUserByEmail(request.email());
        Long memberUserId = toLong(user.get("id"));
        if (memberUserId == null) {
            throw new RuntimeException("Target user not found");
        }

        if (memberRepository.existsByGroupIdAndUserId(groupId, memberUserId)) {
            throw new RuntimeException("User is already a member of this group");
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .userId(memberUserId)
                .role(GroupMember.MemberRole.MEMBER)
                .build();
        member = memberRepository.save(member);

        evictGroupCaches(groupId);
        return toMemberResponse(member, user);
    }

    @Transactional
    public void removeMember(Long groupId, Long memberUserId, Long actorUserId, String actorRole) {
        findGroup(groupId);
        validateAdmin(actorRole);

        GroupMember member = memberRepository.findByGroupIdAndUserId(groupId, memberUserId)
                .orElseThrow(() -> new RuntimeException("User is not a member of this group"));

        if (member.getRole() == GroupMember.MemberRole.OWNER) {
            throw new RuntimeException("Owner cannot be removed from the group");
        }

        memberRepository.delete(member);
        evictGroupCaches(groupId);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long actorUserId, String actorRole) {
        validateAdmin(actorRole);
        LearningGroup group = findGroup(groupId);

        groupRepository.delete(group);
        evictGroupCaches(groupId);
        cacheService.evictByPattern(CacheService.vKey("user:group:all:*"));
        cacheService.evictByPattern(CacheService.vKey("user:group:my:*"));
    }

    @Transactional
    public DiscussionResponse postDiscussion(Long groupId, Long userId, String userRole, PostDiscussionRequest request) {
        LearningGroup group = findGroup(groupId);
        if (!isAdmin(userRole) && !memberRepository.existsByGroupIdAndUserId(groupId, userId))
            throw new RuntimeException("Must be a member to post");

        Discussion parent = null;
        if (request.parentId() != null) {
            parent = discussionRepository.findById(request.parentId())
                    .orElseThrow(() -> new RuntimeException("Parent discussion not found"));
            if (!Objects.equals(parent.getGroup().getId(), groupId)) {
                throw new RuntimeException("Parent discussion does not belong to this group");
            }
        }

        Discussion discussion = Discussion.builder().group(group).authorId(userId)
            .title(request.title()).content(request.content()).parent(parent)
            .createdAt(Instant.now())
            .build();
        discussion = discussionRepository.save(discussion);

        cacheService.evictByPattern(CacheService.vKey("user:group:" + groupId + ":discussions:*"));

        Map<String, Object> author = authServiceClient.getUserById(userId);
        String authorName = extractDisplayName(author);
        String authorRole = asText(author.get("role"), ROLE_LEARNER);
        return GroupMapper.toDiscussionResponse(discussion, authorName, authorRole, 0, isAdmin(userRole));
    }

    @Transactional
    public void deleteDiscussion(Long groupId, Long discussionId, Long actorUserId, String actorRole) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));

        if (!Objects.equals(discussion.getGroup().getId(), groupId)) {
            throw new RuntimeException("Discussion does not belong to this group");
        }

        if (!ROLE_ADMIN.equals(actorRole) && !memberRepository.existsByGroupIdAndUserId(groupId, actorUserId)) {
            throw new RuntimeException("Must be a member to delete messages");
        }

        if (!canDeleteDiscussion(discussion, actorUserId, actorRole)) {
            throw new RuntimeException("You are not allowed to delete this message");
        }

        if (discussionRepository.countByParentId(discussionId) > 0) {
            throw new RuntimeException("Cannot delete a message that has replies");
        }

        discussionRepository.delete(discussion);
        cacheService.evictByPattern(CacheService.vKey("user:group:" + groupId + ":discussions:*"));
    }

    @Transactional
    public void deleteDiscussionById(Long discussionId, Long actorUserId, String actorRole) {
        Discussion discussion = discussionRepository.findById(discussionId)
                .orElseThrow(() -> new RuntimeException("Discussion not found"));
        deleteDiscussion(discussion.getGroup().getId(), discussionId, actorUserId, actorRole);
    }

    private LearningGroup findGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Group not found: " + id));
    }

    private void validateCanCreateGroup(String role) {
        if (!ROLE_ADMIN.equals(role)) {
            throw new RuntimeException("Only admins can create groups");
        }
    }

    private boolean canDeleteDiscussion(Discussion discussion, Long actorUserId, String actorRole) {
        if (isAdmin(actorRole)) {
            return true;
        }

        if (Objects.equals(discussion.getAuthorId(), actorUserId)) {
            return true;
        }

        if (ROLE_MENTOR.equals(actorRole)) {
            Map<String, Object> author = resolveUserById(discussion.getAuthorId());
            String authorRole = asText(author.get("role"), "");
            return ROLE_LEARNER.equalsIgnoreCase(authorRole);
        }

        return false;
    }

    private void validateAdmin(String actorRole) {
        if (!isAdmin(actorRole)) {
            throw new RuntimeException("Only admins can perform this action");
        }
    }

    private boolean isAdmin(String actorRole) {
        return ROLE_ADMIN.equals(actorRole);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> resolveUserByEmail(String email) {
        Map<String, Object> usersPage = authServiceClient.getAllUsers(0, 50, null, email);
        Object contentObj = usersPage.get("content");
        if (!(contentObj instanceof List<?> content)) {
            throw new RuntimeException("Target user not found");
        }

        return content.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .filter(item -> email.equalsIgnoreCase(asText(item.get("email"), "")))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Target user not found"));
    }

    private GroupMemberResponse toMemberResponse(GroupMember member, Map<String, Object> user) {
        return new GroupMemberResponse(
                member.getId(),
                member.getUserId(),
                extractDisplayName(user),
                asText(user.get("email"), "unknown@skillsync"),
                member.getRole().name(),
                member.getJoinedAt()
        );
    }

    private Map<String, Object> resolveUserById(Long userId) {
        return authServiceClient.getUserById(userId);
    }

    private void evictGroupCaches(Long groupId) {
        cacheService.evict(CacheService.vKey("user:group:" + groupId));
        cacheService.evictByPattern(CacheService.vKey("user:group:" + groupId + ":discussions:*"));
        cacheService.evictByPattern(CacheService.vKey("user:group:all:*"));
        cacheService.evictByPattern(CacheService.vKey("user:group:my:*"));
    }

    private String extractDisplayName(Map<String, Object> user) {
        String firstName = asText(user.get("firstName"), "").trim();
        String lastName = asText(user.get("lastName"), "").trim();
        String fullName = (firstName + " " + lastName).trim();

        if (!fullName.isEmpty()) {
            return fullName;
        }

        return asText(user.get("email"), "User");
    }

    private String asText(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? fallback : text;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "General";
        }
        return category.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }
        String normalized = description.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
