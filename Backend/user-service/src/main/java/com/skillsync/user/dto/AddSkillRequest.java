package com.skillsync.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddSkillRequest(
        @NotNull Long skillId,
        @NotBlank String proficiency
) {}
