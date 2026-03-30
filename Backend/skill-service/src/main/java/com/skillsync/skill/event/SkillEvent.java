package com.skillsync.skill.event;

/**
 * Event published when a skill is created, updated, or deactivated.
 * Consumed by other services for event-driven cache invalidation.
 */
public record SkillEvent(
        Long skillId,
        String name,
        String action  // CREATED, UPDATED, DEACTIVATED
) {}
