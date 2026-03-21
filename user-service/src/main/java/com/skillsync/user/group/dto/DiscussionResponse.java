package com.skillsync.user.group.dto;

import java.time.LocalDateTime;

public record DiscussionResponse(Long id, Long groupId, Long authorId, String content, Long parentId, LocalDateTime createdAt) {}
