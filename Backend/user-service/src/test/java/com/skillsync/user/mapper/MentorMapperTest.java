package com.skillsync.user.mapper;

import com.skillsync.user.dto.MentorProfileResponse;
import com.skillsync.user.entity.AvailabilitySlot;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.entity.MentorSkill;
import com.skillsync.user.enums.MentorStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MentorMapperTest {

    @Test @DisplayName("toResponse - full data")
    void toResponseFull() {
        MentorProfile profile = MentorProfile.builder()
                .id(1L).userId(10L).bio("bio").experienceYears(5)
                .hourlyRate(new BigDecimal("50.00"))
                .status(MentorStatus.APPROVED)
                .skills(List.of(new MentorSkill(1L, null, 100L)))
                .slots(List.of(AvailabilitySlot.builder().id(1L).dayOfWeek(1).startTime(LocalTime.of(9,0)).endTime(LocalTime.of(10,0)).isActive(true).build()))
                .build();

        MentorProfileResponse resp = MentorMapper.toResponse(profile);
        assertNotNull(resp);
        assertEquals(1, resp.skills().size());
        assertEquals(1, resp.availability().size());
        assertEquals(new BigDecimal("50.00"), resp.hourlyRate());
    }

    @Test @DisplayName("toResponse - null collections and hourly rate")
    void toResponseNulls() {
        MentorProfile profile = MentorProfile.builder()
                .id(1L).userId(10L).bio("bio").experienceYears(5)
                .hourlyRate(null)
                .status(MentorStatus.PENDING)
                .skills(null)
                .slots(null)
                .build();

        MentorProfileResponse resp = MentorMapper.toResponse(profile);
        assertNotNull(resp);
        assertTrue(resp.skills().isEmpty());
        assertTrue(resp.availability().isEmpty());
        assertEquals(BigDecimal.ZERO, resp.hourlyRate());
    }
}
