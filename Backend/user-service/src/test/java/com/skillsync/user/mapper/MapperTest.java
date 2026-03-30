package com.skillsync.user.mapper;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.*;
import com.skillsync.user.enums.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for all user-service Mapper classes.
 * Verifies pure function behavior with no Spring context needed.
 *
 * NOTE: PaymentMapper tests have been moved to payment-service.
 */
class MapperTest {

    // ────────────────────────────────────────────
    //  UserMapper Tests
    // ────────────────────────────────────────────

    @Nested
    @DisplayName("UserMapper")
    class UserMapperTests {

        @Test
        @DisplayName("should map Profile entity to ProfileResponse with skills")
        void mapProfile_withSkills() {
            Profile profile = Profile.builder()
                    .id(1L).userId(100L)
                    .firstName("John").lastName("Doe")
                    .bio("Developer").phone("1234567890")
                    .location("NYC").profileCompletePct(80)
                    .build();

            List<SkillSummary> skills = List.of(
                    new SkillSummary(1L, "Java", "Backend"),
                    new SkillSummary(2L, "React", "Frontend")
            );

            ProfileResponse response = UserMapper.toProfileResponse(profile, skills);

            assertEquals(1L, response.id());
            assertEquals(100L, response.userId());
            assertEquals("John", response.firstName());
            assertEquals("Doe", response.lastName());
            assertEquals("Developer", response.bio());
            assertEquals("1234567890", response.phone());
            assertEquals("NYC", response.location());
            assertEquals(80, response.profileCompletePct());
            assertEquals(2, response.skills().size());
            assertEquals("Java", response.skills().get(0).name());
        }

        @Test
        @DisplayName("should map Profile with empty skills list")
        void mapProfile_emptySkills() {
            Profile profile = Profile.builder()
                    .id(2L).userId(200L)
                    .firstName("Jane")
                    .build();

            ProfileResponse response = UserMapper.toProfileResponse(profile, List.of());

            assertEquals(2L, response.id());
            assertEquals(200L, response.userId());
            assertTrue(response.skills().isEmpty());
        }
    }

    // ────────────────────────────────────────────
    //  MentorMapper Tests
    // ────────────────────────────────────────────

    @Nested
    @DisplayName("MentorMapper")
    class MentorMapperTests {

        @Test
        @DisplayName("should map MentorProfile with skills and slots")
        void mapMentor_full() {
            MentorProfile profile = MentorProfile.builder()
                    .id(10L).userId(100L)
                    .bio("Expert mentor")
                    .experienceYears(5).hourlyRate(BigDecimal.valueOf(50.0))
                    .avgRating(4.5).totalReviews(20).totalSessions(30)
                    .status(MentorStatus.APPROVED)
                    .skills(List.of(
                            MentorSkill.builder().skillId(1L).build(),
                            MentorSkill.builder().skillId(2L).build()
                    ))
                    .slots(List.of())
                    .build();

            MentorProfileResponse response = MentorMapper.toResponse(profile);

            assertEquals(10L, response.id());
            assertEquals(100L, response.userId());
            assertEquals("Expert mentor", response.bio());
            assertEquals(5, response.experienceYears());
            assertEquals(4.5, response.avgRating());
            assertEquals("APPROVED", response.status());
            assertEquals(2, response.skills().size());
        }

        @Test
        @DisplayName("should handle null skills and slots")
        void mapMentor_nullCollections() {
            MentorProfile profile = MentorProfile.builder()
                    .id(11L).userId(101L)
                    .status(MentorStatus.PENDING)
                    .build();

            MentorProfileResponse response = MentorMapper.toResponse(profile);

            assertNotNull(response);
            assertTrue(response.skills().isEmpty());
            assertTrue(response.availability().isEmpty());
        }
    }

    // ────────────────────────────────────────────
    //  GroupMapper Tests
    // ────────────────────────────────────────────

    @Nested
    @DisplayName("GroupMapper")
    class GroupMapperTests {

        @Test
        @DisplayName("should map LearningGroup to GroupResponse")
        void mapGroup() {
            LearningGroup group = LearningGroup.builder()
                    .id(5L).name("Java Study Group")
                    .description("Learn Java together")
                    .maxMembers(10).createdBy(100L)
                    .build();

            GroupResponse response = GroupMapper.toResponse(group, 3);

            assertEquals(5L, response.id());
            assertEquals("Java Study Group", response.name());
            assertEquals("Learn Java together", response.description());
            assertEquals(10, response.maxMembers());
            assertEquals(3, response.memberCount());
            assertEquals(100L, response.createdBy());
        }
    }
}
