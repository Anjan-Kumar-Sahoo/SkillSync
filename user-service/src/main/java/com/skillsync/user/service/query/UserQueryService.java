package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.entity.UserSkill;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * CQRS Query Service for User/Profile operations.
 * Implements cache-aside pattern with:
 * - Stampede protection (getOrLoad with per-key locking)
 * - Cache penetration protection (null-sentinel caching)
 * - Versioned keys (v1 prefix)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserQueryService {

    private final ProfileRepository profileRepository;
    private final UserSkillRepository userSkillRepository;
    private final SkillServiceClient skillServiceClient;
    private final CacheService cacheService;

    @Value("${cache.ttl.profile:600}")
    private long profileTtl;

    /**
     * Cache-aside with stampede protection: only ONE thread loads from DB on miss.
     */
    public ProfileResponse getProfile(Long userId) {
        String cacheKey = CacheService.vKey("user:profile:" + userId);

        return cacheService.getOrLoad(cacheKey, ProfileResponse.class,
                Duration.ofSeconds(profileTtl), () -> {
                    Profile profile = profileRepository.findByUserId(userId).orElse(null);
                    if (profile == null) return null; // Will be cached as null-sentinel
                    List<SkillSummary> skills = getSkillsForUser(userId);
                    return mapToResponse(profile, skills);
                });
    }

    /**
     * Cache-aside by profile ID with stampede protection.
     */
    public ProfileResponse getProfileById(Long profileId) {
        String cacheKey = CacheService.vKey("user:profile:id:" + profileId);

        return cacheService.getOrLoad(cacheKey, ProfileResponse.class,
                Duration.ofSeconds(profileTtl), () -> {
                    Profile profile = profileRepository.findById(profileId).orElse(null);
                    if (profile == null) return null;
                    List<SkillSummary> skills = getSkillsForUser(profile.getUserId());
                    return mapToResponse(profile, skills);
                });
    }

    /**
     * Paginated queries are NOT cached (pagination + filter combos are too variable).
     */
    public Page<Profile> getAllProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }

    private List<SkillSummary> getSkillsForUser(Long userId) {
        List<UserSkill> userSkills = userSkillRepository.findByUserId(userId);
        List<SkillSummary> skills = new ArrayList<>();
        for (UserSkill us : userSkills) {
            try {
                skills.add(skillServiceClient.getSkillById(us.getSkillId()));
            } catch (Exception e) {
                log.warn("Could not fetch skill {}: {}", us.getSkillId(), e.getMessage());
                skills.add(new SkillSummary(us.getSkillId(), "Unknown", null));
            }
        }
        return skills;
    }

    /**
     * Shared mapper — also used by UserCommandService for return values.
     */
    public static ProfileResponse mapToResponse(Profile profile, List<SkillSummary> skills) {
        return new ProfileResponse(
                profile.getId(), profile.getUserId(),
                profile.getFirstName(), profile.getLastName(),
                null, profile.getBio(), profile.getAvatarUrl(),
                profile.getPhone(), profile.getLocation(),
                profile.getProfileCompletePct(), skills,
                profile.getCreatedAt()
        );
    }
}
