package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.mapper.MentorMapper;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.repository.MentorProfileRepository;
import com.skillsync.user.enums.MentorStatus;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS Query Service for Mentor operations.
 * Implements cache-aside pattern with stampede + penetration protection.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MentorQueryService {

    private final MentorProfileRepository mentorProfileRepository;
    private final CacheService cacheService;
    private final AuthServiceClient authServiceClient;

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
                    return enrichProfile(MentorMapper.toResponse(profile));
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
                    return enrichProfile(MentorMapper.toResponse(profile));
                });
    }

    public Page<MentorProfileResponse> getPendingApplications(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.PENDING, pageable)
                .map(p -> enrichProfile(MentorMapper.toResponse(p)));
    }

    public Page<MentorProfileResponse> searchMentors(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.APPROVED, pageable)
                .map(p -> enrichProfile(MentorMapper.toResponse(p)));
    }

    private MentorProfileResponse enrichProfile(MentorProfileResponse profile) {
        if (profile == null) return null;
        try {
            Map<String, Object> user = authServiceClient.getUserById(profile.userId());
            return new MentorProfileResponse(
                    profile.id(), profile.userId(),
                    (String) user.get("firstName"),
                    (String) user.get("lastName"),
                    (String) user.get("email"),
                    (String) user.get("avatarUrl"),
                    profile.bio(), profile.experienceYears(),
                    profile.hourlyRate(), profile.avgRating(),
                    profile.totalReviews(), profile.totalSessions(),
                    profile.status(), profile.skills(), profile.availability()
            );
        } catch (Exception e) {
            log.warn("Failed to enrich mentor profile userId {}: {}", profile.userId(), e.getMessage());
            return profile;
        }
    }
}
