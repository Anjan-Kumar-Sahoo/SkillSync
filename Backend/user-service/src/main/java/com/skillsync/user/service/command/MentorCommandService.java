package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.config.RabbitMQConfig;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.AvailabilitySlot;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.entity.MentorSkill;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.event.MentorApprovedEvent;
import com.skillsync.user.event.MentorRejectedEvent;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.mapper.MentorMapper;
import com.skillsync.user.repository.AvailabilitySlotRepository;
import com.skillsync.user.repository.MentorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

/**
 * CQRS Command Service for Mentor operations.
 * Handles all write operations and cache invalidation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MentorCommandService {

    private final MentorProfileRepository mentorProfileRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AuthServiceClient authServiceClient;
    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;

    @Transactional
    public MentorProfileResponse apply(Long userId, MentorApplicationRequest request) {
        if (mentorProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("User already has a mentor application");
        }

        MentorProfile profile = MentorProfile.builder()
                .userId(userId).bio(request.bio())
                .experienceYears(request.experienceYears())
                .hourlyRate(request.hourlyRate())
                .avgRating(0.0).totalReviews(0).totalSessions(0)
                .status(MentorStatus.PENDING)
                .skills(new ArrayList<>()).slots(new ArrayList<>())
                .build();

        profile = mentorProfileRepository.save(profile);

        for (Long skillId : request.skillIds()) {
            MentorSkill skill = MentorSkill.builder()
                    .mentor(profile).skillId(skillId).build();
            profile.getSkills().add(skill);
        }
        profile = mentorProfileRepository.save(profile);

        // Do NOT auto-approve or update role here.
        // The admin will approve via AdminController -> approveMentor()
        // which updates status to APPROVED and changes the user's role.

        cacheService.evictByPattern(CacheService.vKey("user:mentor:pending:*"));
        invalidateMentorCaches(profile.getId(), profile.getUserId());
        log.info("[CQRS:COMMAND] Mentor application submitted (PENDING) for userId: {}. Awaiting admin approval.", userId);
        return MentorMapper.toResponse(profile);
    }

    @Transactional
    public void approveMentor(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + mentorId));

        profile.setStatus(MentorStatus.APPROVED);
        mentorProfileRepository.save(profile);

        // If this fails, it MUST throw an exception to trigger a @Transactional rollback.
        // We cannot allow the profile to be APPROVED if the role update fails, 
        // as that destroys data consistency.
        authServiceClient.updateUserRole(profile.getUserId(), "ROLE_MENTOR");

        rabbitTemplate.convertAndSend(RabbitMQConfig.MENTOR_EXCHANGE, "mentor.approved",
                new MentorApprovedEvent(mentorId, profile.getUserId(), null));

        // Invalidate caches
        invalidateMentorCaches(mentorId, profile.getUserId());
        log.info("[CQRS:COMMAND] Mentor {} approved. Cache invalidated.", mentorId);
    }

    @Transactional
    public void rejectMentor(Long mentorId, String reason) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + mentorId));

        profile.setStatus(MentorStatus.REJECTED);
        profile.setRejectionReason(reason);
        mentorProfileRepository.save(profile);

        rabbitTemplate.convertAndSend(RabbitMQConfig.MENTOR_EXCHANGE, "mentor.rejected",
                new MentorRejectedEvent(mentorId, profile.getUserId(), reason));

        invalidateMentorCaches(mentorId, profile.getUserId());
        log.info("[CQRS:COMMAND] Mentor {} rejected. Cache invalidated.", mentorId);
    }



    @Transactional
    public AvailabilitySlotResponse addAvailability(Long userId, AvailabilitySlotRequest request) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Mentor profile not found"));

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .mentor(profile).dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime()).endTime(request.endTime())
                .isActive(true).build();
        slot = availabilitySlotRepository.save(slot);

        invalidateMentorCaches(profile.getId(), userId);
        return new AvailabilitySlotResponse(slot.getId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(), slot.isActive());
    }

    @Transactional
    public void removeAvailability(Long slotId) {
        AvailabilitySlot slot = availabilitySlotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found"));
        MentorProfile profile = slot.getMentor();

        availabilitySlotRepository.delete(slot);

        invalidateMentorCaches(profile.getId(), profile.getUserId());
        log.info("[CQRS:COMMAND] Availability removed for mentorId {}. Cache invalidated.", profile.getId());
    }

    public void updateAvgRating(Long mentorId, double avgRating, int totalReviews) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + mentorId));
        profile.setAvgRating(avgRating);
        profile.setTotalReviews(totalReviews);
        mentorProfileRepository.save(profile);

        invalidateMentorCaches(mentorId, profile.getUserId());
    }

    private void invalidateMentorCaches(Long mentorId, Long userId) {
        cacheService.evict(CacheService.vKey("user:mentor:" + mentorId));
        cacheService.evict(CacheService.vKey("user:mentor:user:" + userId));
        cacheService.evictByPattern(CacheService.vKey("user:mentor:search:*"));
        cacheService.evictByPattern(CacheService.vKey("user:mentor:pending:*"));
    }
}
