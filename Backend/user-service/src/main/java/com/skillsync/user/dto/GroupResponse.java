package com.skillsync.user.dto;

import java.time.LocalDateTime;

public record GroupResponse(
	Long id,
	String name,
	String description,
	String category,
	int maxMembers,
	int memberCount,
	Long createdBy,
	LocalDateTime createdAt
) {}
