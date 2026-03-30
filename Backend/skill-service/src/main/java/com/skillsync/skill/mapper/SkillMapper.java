package com.skillsync.skill.mapper;

import com.skillsync.skill.dto.SkillResponse;
import com.skillsync.skill.entity.Skill;

/**
 * Pure mapping functions for Skill entities.
 * Used by both SkillCommandService and SkillQueryService (CQRS decoupling).
 */
public final class SkillMapper {

    private SkillMapper() {}

    public static SkillResponse toResponse(Skill skill) {
        return new SkillResponse(
                skill.getId(), skill.getName(), skill.getCategory(),
                skill.getDescription(), skill.isActive()
        );
    }
}
