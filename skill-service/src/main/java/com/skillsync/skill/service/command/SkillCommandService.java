package com.skillsync.skill.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.skill.config.RabbitMQConfig;
import com.skillsync.skill.dto.*;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.event.SkillEvent;
import com.skillsync.skill.repository.SkillRepository;
import com.skillsync.skill.service.query.SkillQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * CQRS Command Service for Skill operations.
 * Handles all write operations, cache invalidation, and event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillCommandService {

    private final SkillRepository skillRepository;
    private final CacheService cacheService;
    private final RabbitTemplate rabbitTemplate;

    public SkillResponse createSkill(CreateSkillRequest request) {
        if (skillRepository.existsByName(request.name())) {
            throw new RuntimeException("Skill already exists: " + request.name());
        }
        Skill skill = Skill.builder().name(request.name()).category(request.category())
                .description(request.description()).isActive(true).build();
        skill = skillRepository.save(skill);

        // Invalidate versioned list caches
        cacheService.evictByPattern(CacheService.vKey("skill:all:*"));
        cacheService.evictByPattern(CacheService.vKey("skill:search:*"));

        // Publish event for cross-service cache invalidation
        publishEvent(skill, "CREATED", "skill.created");

        log.info("[CQRS:COMMAND] Skill created: {}. Cache invalidated.", skill.getId());
        return SkillQueryService.mapToResponse(skill);
    }

    public SkillResponse updateSkill(Long id, CreateSkillRequest request) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        skill.setName(request.name());
        skill.setCategory(request.category());
        skill.setDescription(request.description());
        skill = skillRepository.save(skill);

        // Invalidate versioned specific + list caches
        cacheService.evict(CacheService.vKey("skill:" + id));
        cacheService.evictByPattern(CacheService.vKey("skill:all:*"));
        cacheService.evictByPattern(CacheService.vKey("skill:search:*"));

        publishEvent(skill, "UPDATED", "skill.updated");

        log.info("[CQRS:COMMAND] Skill updated: {}. Cache invalidated.", id);
        return SkillQueryService.mapToResponse(skill);
    }

    public void deactivateSkill(Long id) {
        Skill skill = skillRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found: " + id));
        skill.setActive(false);
        skillRepository.save(skill);

        cacheService.evict(CacheService.vKey("skill:" + id));
        cacheService.evictByPattern(CacheService.vKey("skill:all:*"));
        cacheService.evictByPattern(CacheService.vKey("skill:search:*"));

        publishEvent(skill, "DEACTIVATED", "skill.updated");

        log.info("[CQRS:COMMAND] Skill deactivated: {}. Cache invalidated.", id);
    }

    private void publishEvent(Skill skill, String action, String routingKey) {
        try {
            SkillEvent event = new SkillEvent(skill.getId(), skill.getName(), action);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SKILL_EXCHANGE, routingKey, event);
            log.info("[EVENT] Published {} event for skill: {}", action, skill.getId());
        } catch (Exception e) {
            log.error("[EVENT] Failed to publish skill event: {}", e.getMessage());
        }
    }
}
