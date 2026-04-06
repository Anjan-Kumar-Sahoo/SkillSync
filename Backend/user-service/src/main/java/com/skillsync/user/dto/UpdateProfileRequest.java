package com.skillsync.user.dto;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String firstName,
        @Size(min = 2, max = 100) String lastName,
        @Size(max = 1000) String bio,
        @Size(max = 1000) String avatarUrl,
        @Size(max = 20) String phone,
        @Size(max = 200) String location
) {}
