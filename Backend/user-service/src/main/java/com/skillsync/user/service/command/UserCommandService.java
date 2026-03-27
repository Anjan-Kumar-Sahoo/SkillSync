package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.entity.UserSkill;
import com.skillsync.user.mapper.UserMapper;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CQRS Command Service for User/Profile operations.
 * Handles all write operations and cache invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserCommandService {

    private final ProfileRepository profileRepository;
    private final UserSkillRepository userSkillRepository;
    private final CacheService cacheService;

    @Transactional
    public ProfileResponse createOrUpdateProfile(Long userId, UpdateProfileRequest request) {
        Profile profile = profileRepository.findByUserId(userId)
                .orElse(Profile.builder().userId(userId).build());

        if (request.firstName() != null) profile.setFirstName(request.firstName());
        if (request.lastName() != null) profile.setLastName(request.lastName());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.phone() != null) profile.setPhone(request.phone());
        if (request.location() != null) profile.setLocation(request.location());

        profile.setProfileCompletePct(calculateCompleteness(profile));
        profile = profileRepository.save(profile);

        // Invalidate versioned caches
        cacheService.evict(CacheService.vKey("user:profile:" + userId));
        cacheService.evict(CacheService.vKey("user:profile:id:" + profile.getId()));
        log.info("[CQRS:COMMAND] Profile updated for userId: {}. Cache invalidated.", userId);

        return UserMapper.toProfileResponse(profile, List.of());
    }

    @Transactional
    public void addSkill(Long userId, AddSkillRequest request) {
        if (userSkillRepository.existsByUserIdAndSkillId(userId, request.skillId())) {
            throw new RuntimeException("Skill already added to profile");
        }
        UserSkill userSkill = UserSkill.builder()
                .userId(userId)
                .skillId(request.skillId())
                .proficiency(UserSkill.Proficiency.valueOf(request.proficiency()))
                .build();
        userSkillRepository.save(userSkill);

        // Invalidate versioned profile cache (skills are part of profile response)
        cacheService.evict(CacheService.vKey("user:profile:" + userId));
        log.info("[CQRS:COMMAND] Skill {} added for userId: {}. Cache invalidated.", request.skillId(), userId);
    }

    @Transactional
    public void removeSkill(Long userId, Long skillId) {
        userSkillRepository.deleteByUserIdAndSkillId(userId, skillId);

        // Invalidate versioned profile cache
        cacheService.evict(CacheService.vKey("user:profile:" + userId));
        log.info("[CQRS:COMMAND] Skill {} removed for userId: {}. Cache invalidated.", skillId, userId);
    }

    private int calculateCompleteness(Profile profile) {
        int score = 0;
        if (profile.getFirstName() != null) score += 20;
        if (profile.getLastName() != null) score += 20;
        if (profile.getBio() != null) score += 20;
        if (profile.getPhone() != null) score += 20;
        if (profile.getLocation() != null) score += 20;
        return score;
    }
}
