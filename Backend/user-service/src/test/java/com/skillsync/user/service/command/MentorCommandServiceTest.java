package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.AvailabilitySlot;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.entity.MentorSkill;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.event.MentorApprovedEvent;
import com.skillsync.user.event.MentorRejectedEvent;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.repository.AvailabilitySlotRepository;
import com.skillsync.user.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorCommandService Unit Tests")
class MentorCommandServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;

    @InjectMocks private MentorCommandService mentorCommandService;

    private static final Long USER_ID = 100L;
    private static final Long MENTOR_ID = 1L;
    private MentorProfile mentorProfile;

    @BeforeEach
    void setUp() {
        mentorProfile = MentorProfile.builder()
                .id(MENTOR_ID)
                .userId(USER_ID)
                .bio("Existing bio")
                .experienceYears(3)
                .hourlyRate(new BigDecimal("50.00"))
                .status(MentorStatus.REJECTED)
                .skills(new ArrayList<>())
                .slots(new ArrayList<>())
                .build();
    }

    @Nested
    @DisplayName("apply() tests")
    class ApplyTests {

        @Test
        @DisplayName("New Application: Success")
        void apply_NewProfile_Success() {
            // Arrange
            MentorApplicationRequest request = new MentorApplicationRequest("New Bio", 5, new BigDecimal("100.00"), List.of(10L, 20L));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(mentorProfileRepository.save(any(MentorProfile.class))).thenAnswer(i -> i.getArgument(0));

            // Act
            MentorProfileResponse response = mentorCommandService.apply(USER_ID, request);

            // Assert
            assertThat(response.status()).isEqualTo(MentorStatus.PENDING.name());
            
            ArgumentCaptor<MentorProfile> captor = ArgumentCaptor.forClass(MentorProfile.class);
            verify(mentorProfileRepository, times(2)).save(captor.capture());
            
            MentorProfile saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getBio()).isEqualTo("New Bio");
            assertThat(saved.getStatus()).isEqualTo(MentorStatus.PENDING);
            assertThat(saved.getSkills()).hasSize(2);
            
            verify(cacheService, times(2)).evict(anyString());
            verify(cacheService, times(3)).evictByPattern(anyString());
        }

        @Test
        @DisplayName("Re-application: Clears existing skills")
        void apply_ExistingProfile_ClearsSkills() {
            // Arrange
            mentorProfile.getSkills().add(MentorSkill.builder().skillId(99L).build());
            MentorApplicationRequest request = new MentorApplicationRequest("Updated Bio", 4, new BigDecimal("60.00"), List.of(10L));
            
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(mentorProfile);

            // Act
            mentorCommandService.apply(USER_ID, request);

            // Assert
            assertThat(mentorProfile.getSkills()).hasSize(1);
            assertThat(mentorProfile.getSkills().get(0).getSkillId()).isEqualTo(10L);
            assertThat(mentorProfile.getBio()).isEqualTo("Updated Bio");
        }

        @Test
        @DisplayName("Re-application: Handles null skills gracefully")
        void apply_ExistingProfile_NullSkills_Initializes() {
            // Arrange
            mentorProfile.setSkills(null);
            MentorApplicationRequest request = new MentorApplicationRequest("Bio", 3, BigDecimal.TEN, List.of(10L));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(mentorProfile);

            // Act
            mentorCommandService.apply(USER_ID, request);

            // Assert
            assertThat(mentorProfile.getSkills()).hasSize(1);
        }

        @Test
        @DisplayName("Re-application: Throws if Pending")
        void apply_Pending_ThrowsException() {
            mentorProfile.setStatus(MentorStatus.PENDING);
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            assertThatThrownBy(() -> mentorCommandService.apply(USER_ID, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already pending");
        }

        @Test
        @DisplayName("Re-application: Throws if already Approved")
        void apply_Approved_ThrowsException() {
            mentorProfile.setStatus(MentorStatus.APPROVED);
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            assertThatThrownBy(() -> mentorCommandService.apply(USER_ID, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already an approved mentor");
        }
    }

    @Nested
    @DisplayName("approveMentor() tests")
    class ApproveMentorTests {
        @Test
        @DisplayName("Success: Updates status and role")
        void approveMentor_Success() {
            mentorProfile.setStatus(MentorStatus.PENDING);
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.approveMentor(MENTOR_ID);

            assertThat(mentorProfile.getStatus()).isEqualTo(MentorStatus.APPROVED);
            verify(authServiceClient).updateUserRole(USER_ID, "ROLE_MENTOR");
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.approved"), any(MentorApprovedEvent.class));
        }

        @Test
        @DisplayName("Failure: Mentor not found")
        void approveMentor_NotFound() {
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.empty());
            assertThatThrownBy(() -> mentorCommandService.approveMentor(MENTOR_ID))
                    .isInstanceOf(RuntimeException.class);
        }
    }

    @Nested
    @DisplayName("rejectMentor() tests")
    class RejectMentorTests {
        @Test
        @DisplayName("Success: Updates status and reason")
        void rejectMentor_Success() {
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.rejectMentor(MENTOR_ID, "Insufficient experience");

            assertThat(mentorProfile.getStatus()).isEqualTo(MentorStatus.REJECTED);
            assertThat(mentorProfile.getRejectionReason()).isEqualTo("Insufficient experience");
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.rejected"), any(MentorRejectedEvent.class));
        }
    }

    @Nested
    @DisplayName("promoteUserToMentor() tests")
    class PromoteTests {
        @Test
        @DisplayName("Existing profile: Updates status")
        void promote_Existing_Updates() {
            mentorProfile.setStatus(MentorStatus.SUSPENDED);
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(mentorProfileRepository.save(any())).thenReturn(mentorProfile);

            mentorCommandService.promoteUserToMentor(USER_ID);

            assertThat(mentorProfile.getStatus()).isEqualTo(MentorStatus.APPROVED);
            verify(authServiceClient).updateUserRole(USER_ID, "ROLE_MENTOR");
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.promoted"), any(Map.class));
        }

        @Test
        @DisplayName("No profile: Creates new approved profile")
        void promote_New_Creates() {
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(mentorProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            mentorCommandService.promoteUserToMentor(USER_ID);

            verify(mentorProfileRepository).save(argThat(p -> p.getStatus() == MentorStatus.APPROVED));
            verify(authServiceClient).updateUserRole(USER_ID, "ROLE_MENTOR");
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.promoted"), any(Map.class));
        }
    }

    @Nested
    @DisplayName("demoteUserToLearner() tests")
    class DemoteTests {
        @Test
        @DisplayName("Profile exists: Suspends and sends event with reason")
        void demote_WithProfile_Success() {
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.demoteUserToLearner(USER_ID, "Behavioral issues");

            assertThat(mentorProfile.getStatus()).isEqualTo(MentorStatus.SUSPENDED);
            assertThat(mentorProfile.getRejectionReason()).isEqualTo("Behavioral issues");
            
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.demoted"), eventCaptor.capture());
            
            assertThat(eventCaptor.getValue()).containsEntry("reason", "Behavioral issues");
            verify(authServiceClient).updateUserRole(USER_ID, "ROLE_LEARNER");
        }

        @Test
        @DisplayName("Profile absent: Still updates role and sends default reason")
        void demote_NoProfile_Success() {
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            mentorCommandService.demoteUserToLearner(USER_ID, "");

            verify(authServiceClient).updateUserRole(USER_ID, "ROLE_LEARNER");
            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.demoted"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().get("reason").toString()).contains("Role changed to learner");
        }

        @Test
        @DisplayName("Reason is null: Uses default reason")
        void demote_NullReason_Success() {
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            mentorCommandService.demoteUserToLearner(USER_ID, null);

            ArgumentCaptor<Map<String, Object>> eventCaptor = ArgumentCaptor.forClass(Map.class);
            verify(rabbitTemplate).convertAndSend(eq("mentor.exchange"), eq("mentor.demoted"), eventCaptor.capture());
            assertThat(eventCaptor.getValue().get("reason").toString()).contains("Role changed to learner");
        }
    }

    @Nested
    @DisplayName("addAvailability() tests")
    class AddAvailabilityTests {
        @Test
        @DisplayName("Success path")
        void addAvailability_Success() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end = LocalTime.of(11, 0);
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, start, end);
            
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(availabilitySlotRepository.existsByMentor_IdAndDayOfWeekAndStartTimeAndEndTime(anyLong(), anyInt(), any(), any())).thenReturn(false);
            when(availabilitySlotRepository.save(any())).thenAnswer(i -> {
                AvailabilitySlot s = i.getArgument(0);
                s.setId(500L);
                return s;
            });

            AvailabilitySlotResponse response = mentorCommandService.addAvailability(USER_ID, request);

            assertThat(response.id()).isEqualTo(500L);
            verify(availabilitySlotRepository).save(any());
        }

        @Test
        @DisplayName("Edge Case: Exactly 30 minutes (Min)")
        void addAvailability_MinDuration_Success() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end = LocalTime.of(10, 30);
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, start, end);
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(availabilitySlotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            mentorCommandService.addAvailability(USER_ID, request);
            verify(availabilitySlotRepository).save(any());
        }

        @Test
        @DisplayName("Edge Case: Exactly 120 minutes (Max)")
        void addAvailability_MaxDuration_Success() {
            LocalTime start = LocalTime.of(10, 0);
            LocalTime end = LocalTime.of(12, 0);
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, start, end);
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(availabilitySlotRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            mentorCommandService.addAvailability(USER_ID, request);
            verify(availabilitySlotRepository).save(any());
        }

        @Test
        @DisplayName("Failure: End before Start")
        void addAvailability_InvalidTimes() {
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, LocalTime.of(10, 0), LocalTime.of(0, 0));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            assertThatThrownBy(() -> mentorCommandService.addAvailability(USER_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("End time must be after start time");
        }

        @Test
        @DisplayName("Failure: Duration < 30m")
        void addAvailability_TooShort() {
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, LocalTime.of(10, 0), LocalTime.of(10, 29));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            assertThatThrownBy(() -> mentorCommandService.addAvailability(USER_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("between 30 and 120 minutes");
        }

        @Test
        @DisplayName("Failure: Duration > 120m")
        void addAvailability_TooLong() {
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, LocalTime.of(10, 0), LocalTime.of(12, 1));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));

            assertThatThrownBy(() -> mentorCommandService.addAvailability(USER_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("between 30 and 120 minutes");
        }

        @Test
        @DisplayName("Failure: Slot already exists")
        void addAvailability_Duplicate() {
            AvailabilitySlotRequest request = new AvailabilitySlotRequest(1, LocalTime.of(10, 0), LocalTime.of(11, 0));
            when(mentorProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(mentorProfile));
            when(availabilitySlotRepository.existsByMentor_IdAndDayOfWeekAndStartTimeAndEndTime(anyLong(), anyInt(), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> mentorCommandService.addAvailability(USER_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("updateMentorMetrics() tests")
    class UpdateMetricsTests {
        @Test
        @DisplayName("By ID: Success with totalSessions")
        void updateMetrics_ById_Success() {
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.updateMentorMetrics(MENTOR_ID, 4.5, 10, 50L);

            assertThat(mentorProfile.getAvgRating()).isEqualTo(4.5);
            assertThat(mentorProfile.getTotalReviews()).isEqualTo(10);
            assertThat(mentorProfile.getTotalSessions()).isEqualTo(50);
            verify(mentorProfileRepository).save(mentorProfile);
        }

        @Test
        @DisplayName("By ID: Success with null sessions (remains unchanged)")
        void updateMetrics_NullSessions_Unchanged() {
            mentorProfile.setTotalSessions(100);
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.updateMentorMetrics(MENTOR_ID, 4.0, 5, null);

            assertThat(mentorProfile.getTotalSessions()).isEqualTo(100);
        }

        @Test
        @DisplayName("Fallback: Find by UserId when findById fails")
        void updateMetrics_FallbackUserId_Success() {
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.empty());
            when(mentorProfileRepository.findByUserId(MENTOR_ID)).thenReturn(Optional.of(mentorProfile));

            mentorCommandService.updateMentorMetrics(MENTOR_ID, 4.2, 8, 12L);

            assertThat(mentorProfile.getAvgRating()).isEqualTo(4.2);
            verify(mentorProfileRepository).save(mentorProfile);
        }

        @Test
        @DisplayName("Failure: Not found by ID or UserID")
        void updateMetrics_NotFound_Throws() {
            when(mentorProfileRepository.findById(MENTOR_ID)).thenReturn(Optional.empty());
            when(mentorProfileRepository.findByUserId(MENTOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> mentorCommandService.updateMentorMetrics(MENTOR_ID, 0, 0, 0L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("not found for identifier");
        }
    }

    @Test
    @DisplayName("removeAvailability: Success")
    void removeAvailability_Success() {
        AvailabilitySlot slot = AvailabilitySlot.builder().id(999L).mentor(mentorProfile).build();
        when(availabilitySlotRepository.findById(999L)).thenReturn(Optional.of(slot));

        mentorCommandService.removeAvailability(999L);

        verify(availabilitySlotRepository).delete(slot);
        verify(cacheService, atLeastOnce()).evict(anyString());
    }
}