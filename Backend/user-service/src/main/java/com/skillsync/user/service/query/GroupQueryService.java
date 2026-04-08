package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
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
import java.util.Map;

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
    public GroupResponse getGroupById(Long id) {
        String cacheKey = CacheService.vKey("user:group:" + id);

        return cacheService.getOrLoad(cacheKey, GroupResponse.class,
                Duration.ofSeconds(groupTtl), () -> {
                    LearningGroup group = groupRepository.findById(id).orElse(null);
                    if (group == null) return null;
                    int count = group.getMembers() != null ? group.getMembers().size()
                            : (int) memberRepository.countByGroupId(group.getId());
                    return GroupMapper.toResponse(group, count);
                });
    }

    public Page<GroupResponse> getAllGroups(String search, String category, Pageable pageable) {
        return groupRepository.searchGroups(search, category, pageable).map(g -> {
            int count = g.getMembers() != null ? g.getMembers().size()
                    : (int) memberRepository.countByGroupId(g.getId());
            return GroupMapper.toResponse(g, count);
        });
    }

    public Page<GroupResponse> getMyGroups(Long userId, Pageable pageable) {
        return groupRepository.findMyGroups(userId, pageable).map(g -> {
            int count = g.getMembers() != null ? g.getMembers().size()
                    : (int) memberRepository.countByGroupId(g.getId());
            return GroupMapper.toResponse(g, count);
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
        if (!ROLE_ADMIN.equals(userRole) && !memberRepository.existsByGroupIdAndUserId(groupId, userId)) {
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
                            replies);
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
}

