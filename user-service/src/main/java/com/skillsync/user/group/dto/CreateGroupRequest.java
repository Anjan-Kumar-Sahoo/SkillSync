package com.skillsync.user.group.dto;

import jakarta.validation.constraints.*;

public record CreateGroupRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @Min(2) @Max(50) int maxMembers
) {}
