package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * CQRS Query Service for Group operations.
 * Cache-aside with stampede + penetration protection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GroupQueryService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository memberRepository;
    private final DiscussionRepository discussionRepository;
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
                    return mapToResponse(group, count);
                });
    }

    public Page<GroupResponse> getAllGroups(Pageable pageable) {
        return groupRepository.findAll(pageable).map(g -> {
            int count = g.getMembers() != null ? g.getMembers().size()
                    : (int) memberRepository.countByGroupId(g.getId());
            return mapToResponse(g, count);
        });
    }

    public Page<DiscussionResponse> getDiscussions(Long groupId, Pageable pageable) {
        return discussionRepository.findByGroupIdOrderByCreatedAtDesc(groupId, pageable)
                .map(GroupQueryService::mapDiscussion);
    }

    /**
     * Shared mapper — also used by GroupCommandService.
     */
    public static GroupResponse mapToResponse(LearningGroup g, int memberCount) {
        return new GroupResponse(g.getId(), g.getName(), g.getDescription(),
                g.getMaxMembers(), memberCount, g.getCreatedBy(), g.getCreatedAt());
    }

    public static DiscussionResponse mapDiscussion(Discussion d) {
        return new DiscussionResponse(d.getId(), d.getGroup().getId(), d.getAuthorId(),
                d.getContent(), d.getParent() != null ? d.getParent().getId() : null,
                d.getCreatedAt());
    }
}
