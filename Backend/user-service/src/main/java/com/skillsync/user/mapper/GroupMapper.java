package com.skillsync.user.mapper;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;

/**
 * Pure mapping functions for Group/Discussion entities.
 * Used by both GroupCommandService and GroupQueryService (CQRS decoupling).
 */
public final class GroupMapper {

    private GroupMapper() {}

    public static GroupResponse toResponse(LearningGroup group, int memberCount, boolean joined) {
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(), group.getCategory(),
                group.getMaxMembers(), memberCount, group.getCreatedBy(), group.getCreatedAt(), joined);
    }

    public static DiscussionResponse toDiscussionResponse(
            Discussion discussion,
            String authorName,
            String authorRole,
            int replies) {
        return new DiscussionResponse(discussion.getId(), discussion.getGroup().getId(),
                discussion.getAuthorId(), authorName, authorRole, discussion.getTitle(), discussion.getContent(),
                discussion.getParent() != null ? discussion.getParent().getId() : null, replies,
                discussion.getCreatedAt());
    }
}
