package com.skillsync.user.dto;

import java.time.LocalDateTime;

public record DiscussionResponse(
	Long id,
	Long groupId,
	Long authorId,
	String authorName,
	String authorRole,
	String title,
	String content,
	Long parentId,
	int replies,
	LocalDateTime createdAt
) {}
