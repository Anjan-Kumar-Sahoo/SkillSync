package com.skillsync.skill.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.skill.dto.*;
import com.skillsync.skill.entity.Skill;
import com.skillsync.skill.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * CQRS Query Service for Skill operations.
 * Cache-aside with stampede + penetration protection (1-hour TTL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SkillQueryService {

    private final SkillRepository skillRepository;
    private final CacheService cacheService;

    @Value("${cache.ttl.skill:3600}")
    private long skillTtl;

    /**
     * Cache-aside with stampede protection: get skill by ID.
     */
    public SkillResponse getSkillById(Long id) {
        String cacheKey = CacheService.vKey("skill:" + id);

        return cacheService.getOrLoad(cacheKey, SkillResponse.class,
                Duration.ofSeconds(skillTtl), () -> {
                    Skill skill = skillRepository.findById(id).orElse(null);
                    if (skill == null) return null;
                    return mapToResponse(skill);
                });
    }

    /**
     * Paginated — not cached due to filter variability.
     */
    public Page<SkillResponse> getAllSkills(Pageable pageable) {
        return skillRepository.findByIsActiveTrue(pageable).map(SkillQueryService::mapToResponse);
    }

    public List<SkillResponse> searchSkills(String query) {
        return skillRepository.searchByName(query).stream()
                .map(SkillQueryService::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Shared mapper — also used by SkillCommandService.
     */
    public static SkillResponse mapToResponse(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getName(), skill.getCategory(),
                skill.getDescription(), skill.isActive());
    }
}
