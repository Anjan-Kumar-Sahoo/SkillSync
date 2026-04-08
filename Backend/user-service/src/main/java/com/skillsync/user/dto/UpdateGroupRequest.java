package com.skillsync.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateGroupRequest(
        @Size(max = 200) String name,
        @Size(max = 2000) String description,
        @Size(max = 100) String category,
        @Min(2) @Max(200) Integer maxMembers
) {}
