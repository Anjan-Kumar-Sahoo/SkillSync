package com.skillsync.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PostDiscussionRequest(
	@NotBlank @Size(max = 150) String title,
	@NotBlank @Size(max = 5000) String content,
	Long parentId
) {}
