package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.repository.MentorProfileRepository;
import com.skillsync.user.enums.MentorStatus;
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
 * CQRS Query Service for Mentor operations.
 * Implements cache-aside pattern with stampede + penetration protection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MentorQueryService {

    private final MentorProfileRepository mentorProfileRepository;
    private final CacheService cacheService;

    @Value("${cache.ttl.mentor:600}")
    private long mentorTtl;

    /**
     * Cache-aside with stampede protection: get mentor by ID.
     */
    public MentorProfileResponse getMentorById(Long id) {
        String cacheKey = CacheService.vKey("user:mentor:" + id);

        return cacheService.getOrLoad(cacheKey, MentorProfileResponse.class,
                Duration.ofSeconds(mentorTtl), () -> {
                    MentorProfile profile = mentorProfileRepository.findById(id).orElse(null);
                    if (profile == null) return null;
                    return mapToResponse(profile);
                });
    }

    /**
     * Cache-aside with stampede protection: get mentor by user ID.
     */
    public MentorProfileResponse getMentorByUserId(Long userId) {
        String cacheKey = CacheService.vKey("user:mentor:user:" + userId);

        return cacheService.getOrLoad(cacheKey, MentorProfileResponse.class,
                Duration.ofSeconds(mentorTtl), () -> {
                    MentorProfile profile = mentorProfileRepository.findByUserId(userId).orElse(null);
                    if (profile == null) return null;
                    return mapToResponse(profile);
                });
    }

    public Page<MentorProfileResponse> getPendingApplications(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.PENDING, pageable)
                .map(MentorQueryService::mapToResponse);
    }

    public Page<MentorProfileResponse> searchMentors(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.APPROVED, pageable)
                .map(MentorQueryService::mapToResponse);
    }

    /**
     * Shared mapper — also used by MentorCommandService.
     */
    public static MentorProfileResponse mapToResponse(MentorProfile profile) {
        List<SkillSummary> skills = profile.getSkills() != null
                ? profile.getSkills().stream()
                    .map(s -> new SkillSummary(s.getSkillId(), null, null))
                    .collect(Collectors.toList())
                : List.of();

        List<AvailabilitySlotResponse> slots = profile.getSlots() != null
                ? profile.getSlots().stream()
                    .map(s -> new AvailabilitySlotResponse(s.getId(), s.getDayOfWeek(),
                            s.getStartTime(), s.getEndTime(), s.isActive()))
                    .collect(Collectors.toList())
                : List.of();

        return new MentorProfileResponse(
                profile.getId(), profile.getUserId(),
                null, null, null,
                profile.getBio(), profile.getExperienceYears(),
                profile.getHourlyRate(), profile.getAvgRating(),
                profile.getTotalReviews(), profile.getTotalSessions(),
                profile.getStatus().name(), skills, slots
        );
    }
}
