package com.skillsync.user.mapper;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Pure mapping functions for MentorProfile entities.
 * Used by both MentorCommandService and MentorQueryService (CQRS decoupling).
 */
public final class MentorMapper {

    private MentorMapper() {}

    public static MentorProfileResponse toResponse(MentorProfile profile) {
        List<SkillSummary> skills = profile.getSkills() != null
                ? profile.getSkills().stream()
                    .map(s -> new SkillSummary(s.getSkillId(), null, null))
                    .collect(Collectors.toList())
                : List.of();

        List<AvailabilitySlotResponse> availability = profile.getSlots() != null
                ? profile.getSlots().stream()
                    .map(MentorMapper::toSlotResponse)
                    .collect(Collectors.toList())
                : List.of();

        return new MentorProfileResponse(
                profile.getId(), profile.getUserId(),
                null, null, null, null,
                profile.getBio(), profile.getExperienceYears(),
                profile.getHourlyRate() != null
                        ? profile.getHourlyRate()
                        : BigDecimal.ZERO,
                profile.getAvgRating(),
                profile.getTotalReviews(), profile.getTotalSessions(),
                profile.getStatus().name(), skills, availability
        );
    }

    public static AvailabilitySlotResponse toSlotResponse(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(
                slot.getId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime(), slot.isActive()
        );
    }
}
