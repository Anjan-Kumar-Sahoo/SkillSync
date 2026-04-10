package com.skillsync.user.dto;

import jakarta.validation.constraints.*;

public record CreateGroupRequest(
    @NotBlank @Size(max = 200) String name,
    @Size(max = 2000) String description,
    @Size(max = 100) String category,
    @Min(2) @Max(200) Integer maxMembers
) {}
