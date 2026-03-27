package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.mapper.MentorMapper;
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
                    return MentorMapper.toResponse(profile);
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
                    return MentorMapper.toResponse(profile);
                });
    }

    public Page<MentorProfileResponse> getPendingApplications(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.PENDING, pageable)
                .map(MentorMapper::toResponse);
    }

    public Page<MentorProfileResponse> searchMentors(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.APPROVED, pageable)
                .map(MentorMapper::toResponse);
    }
}
