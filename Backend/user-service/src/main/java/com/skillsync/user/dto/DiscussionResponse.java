package com.skillsync.user.dto;

import java.time.Instant;

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
	Instant createdAt,
	boolean isAdmin
) {}
