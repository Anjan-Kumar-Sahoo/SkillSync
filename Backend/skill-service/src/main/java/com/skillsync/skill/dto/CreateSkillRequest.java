package com.skillsync.skill.dto;

import jakarta.validation.constraints.NotBlank;
public record CreateSkillRequest(@NotBlank String name, String category, String description) {}
