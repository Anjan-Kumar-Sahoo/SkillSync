package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.exception.ResourceNotFoundException;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.mapper.GroupMapper;
import com.skillsync.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * CQRS Query Service for Group operations.
 * Cache-aside with stampede + penetration protection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupQueryService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_LEARNER = "ROLE_LEARNER";

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final DiscussionRepository discussionRepository;
    private final AuthServiceClient authServiceClient;
    private final CacheService cacheService;

    @Value("${cache.ttl.group:600}")
    private long groupTtl;

    /**
     * Cache-aside with stampede protection: get group by ID.
     */
    public GroupResponse getGroupById(Long id, Long userId) {
        String cacheKey = CacheService.vKey("user:group:" + id);

        GroupResponse base = cacheService.getOrLoad(cacheKey, GroupResponse.class,
                Duration.ofSeconds(groupTtl), () -> {
                    LearningGroup group = groupRepository.findById(id).orElse(null);
                    if (group == null) return null;
                int count = (int) memberRepository.countByGroupId(group.getId());
                    return GroupMapper.toResponse(group, count, false);
                });

        if (base == null) {
            throw new ResourceNotFoundException("Group not found: " + id);
        }

        boolean joined = isJoined(id, userId);
        return withJoined(base, joined);
    }

    public Page<GroupResponse> getAllGroups(String search, String category, Long userId, Pageable pageable) {
        Page<LearningGroup> groupsPage = groupRepository.searchGroups(search, category, pageable);
        Set<Long> joinedGroupIds = resolveJoinedGroupIds(groupsPage.getContent(), userId);

        return groupsPage.map(g -> {
            int count = (int) memberRepository.countByGroupId(g.getId());
            return GroupMapper.toResponse(g, count, joinedGroupIds.contains(g.getId()));
        });
    }

    public Page<GroupResponse> getMyGroups(Long userId, Pageable pageable) {
        return groupRepository.findMyGroups(userId, pageable).map(g -> {
            int count = (int) memberRepository.countByGroupId(g.getId());
            return GroupMapper.toResponse(g, count, true);
        });
    }

    public Page<GroupMemberResponse> getGroupMembers(Long groupId, Pageable pageable) {
        return memberRepository.findByGroupId(groupId, pageable)
                .map(member -> {
                    Map<String, Object> user = authServiceClient.getUserById(member.getUserId());
                    return new GroupMemberResponse(
                            member.getId(),
                            member.getUserId(),
                            extractDisplayName(user),
                            asText(user.get("email"), "unknown@skillsync"),
                            member.getRole().name(),
                            member.getJoinedAt()
                    );
                });
    }

    public Page<DiscussionResponse> getDiscussions(Long groupId, Long userId, String userRole, Pageable pageable) {
        boolean adminViewer = ROLE_ADMIN.equals(userRole);

        if (!adminViewer && !memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Only group members can view messages");
        }

        return discussionRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable)
                .map(discussion -> {
                    Map<String, Object> author = authServiceClient.getUserById(discussion.getAuthorId());
                    int replies = (int) discussionRepository.countByParentId(discussion.getId());
                    String authorRole = asText(author.get("role"), ROLE_LEARNER);
                    return GroupMapper.toDiscussionResponse(
                            discussion,
                            extractDisplayName(author),
                            authorRole,
                            replies,
                            adminViewer);
                });
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

    private Set<Long> resolveJoinedGroupIds(List<LearningGroup> groups, Long userId) {
        if (userId == null || groups == null || groups.isEmpty()) {
            return Set.of();
        }

        List<Long> groupIds = groups.stream()
                .map(LearningGroup::getId)
                .toList();
        return new HashSet<>(memberRepository.findJoinedGroupIds(userId, groupIds));
    }

    private boolean isJoined(Long groupId, Long userId) {
        if (userId == null) {
            return false;
        }
        return memberRepository.existsByGroupIdAndUserId(groupId, userId);
    }

    private GroupResponse withJoined(GroupResponse response, boolean joined) {
        return new GroupResponse(
                response.id(),
                response.name(),
                response.description(),
                response.category(),
                response.maxMembers(),
                response.memberCount(),
                response.createdBy(),
                response.createdAt(),
                joined
        );
    }
}

