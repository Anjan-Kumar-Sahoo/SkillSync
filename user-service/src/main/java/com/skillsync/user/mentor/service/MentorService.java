package com.skillsync.user.mentor.service;

import com.skillsync.user.dto.SkillSummary;
import com.skillsync.user.mentor.config.RabbitMQConfig;
import com.skillsync.user.mentor.dto.*;
import com.skillsync.user.mentor.entity.AvailabilitySlot;
import com.skillsync.user.mentor.entity.MentorProfile;
import com.skillsync.user.mentor.entity.MentorSkill;
import com.skillsync.user.mentor.enums.MentorStatus;
import com.skillsync.user.mentor.event.MentorApprovedEvent;
import com.skillsync.user.mentor.event.MentorRejectedEvent;
import com.skillsync.user.mentor.feign.AuthServiceClient;
import com.skillsync.user.mentor.repository.AvailabilitySlotRepository;
import com.skillsync.user.mentor.repository.MentorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MentorService {

    private final MentorProfileRepository mentorProfileRepository;
    private final AvailabilitySlotRepository availabilitySlotRepository;
    private final AuthServiceClient authServiceClient;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public MentorProfileResponse apply(Long userId, MentorApplicationRequest request) {
        if (mentorProfileRepository.existsByUserId(userId)) {
            throw new RuntimeException("User already has a mentor application");
        }

        MentorProfile profile = MentorProfile.builder()
                .userId(userId)
                .bio(request.bio())
                .experienceYears(request.experienceYears())
                .hourlyRate(request.hourlyRate())
                .avgRating(0.0)
                .totalReviews(0)
                .totalSessions(0)
                .status(MentorStatus.PENDING)
                .skills(new ArrayList<>())
                .slots(new ArrayList<>())
                .build();

        profile = mentorProfileRepository.save(profile);

        // Add skills
        for (Long skillId : request.skillIds()) {
            MentorSkill skill = MentorSkill.builder()
                    .mentor(profile)
                    .skillId(skillId)
                    .build();
            profile.getSkills().add(skill);
        }
        profile = mentorProfileRepository.save(profile);

        log.info("Mentor application submitted for userId: {}", userId);
        return mapToResponse(profile);
    }

    public MentorProfileResponse getMentorById(Long id) {
        MentorProfile profile = mentorProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + id));
        return mapToResponse(profile);
    }

    public MentorProfileResponse getMentorByUserId(Long userId) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Mentor not found for userId: " + userId));
        return mapToResponse(profile);
    }

    public Page<MentorProfileResponse> getPendingApplications(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.PENDING, pageable)
                .map(this::mapToResponse);
    }

    public Page<MentorProfileResponse> searchMentors(Pageable pageable) {
        return mentorProfileRepository.findByStatus(MentorStatus.APPROVED, pageable)
                .map(this::mapToResponse);
    }

    @Transactional
    public void approveMentor(Long mentorId) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + mentorId));

        profile.setStatus(MentorStatus.APPROVED);
        mentorProfileRepository.save(profile);

        // Update user role in Auth Service
        try {
            authServiceClient.updateUserRole(profile.getUserId(), "ROLE_MENTOR");
        } catch (Exception e) {
            log.error("Failed to update role for userId: {}", profile.getUserId(), e);
        }

        // Publish event
        rabbitTemplate.convertAndSend(RabbitMQConfig.MENTOR_EXCHANGE, "mentor.approved",
                new MentorApprovedEvent(mentorId, profile.getUserId(), null));

        log.info("Mentor {} approved", mentorId);
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

        log.info("Mentor {} rejected", mentorId);
    }

    @Transactional
    public AvailabilitySlotResponse addAvailability(Long userId, AvailabilitySlotRequest request) {
        MentorProfile profile = mentorProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Mentor profile not found"));

        AvailabilitySlot slot = AvailabilitySlot.builder()
                .mentor(profile)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isActive(true)
                .build();
        slot = availabilitySlotRepository.save(slot);
        return new AvailabilitySlotResponse(slot.getId(), slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime(), slot.isActive());
    }

    public void removeAvailability(Long slotId) {
        availabilitySlotRepository.deleteById(slotId);
    }

    public void updateAvgRating(Long mentorId, double avgRating, int totalReviews) {
        MentorProfile profile = mentorProfileRepository.findById(mentorId)
                .orElseThrow(() -> new RuntimeException("Mentor not found: " + mentorId));
        profile.setAvgRating(avgRating);
        profile.setTotalReviews(totalReviews);
        mentorProfileRepository.save(profile);
    }

    private MentorProfileResponse mapToResponse(MentorProfile profile) {
        List<SkillSummary> skills = profile.getSkills() != null
                ? profile.getSkills().stream()
                    .map(s -> new SkillSummary(s.getSkillId(), null, null))
                    .collect(Collectors.toList())
                : List.of();

        List<AvailabilitySlotResponse> slots = profile.getSlots() != null
                ? profile.getSlots().stream()
                    .map(s -> new AvailabilitySlotResponse(s.getId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime(), s.isActive()))
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
