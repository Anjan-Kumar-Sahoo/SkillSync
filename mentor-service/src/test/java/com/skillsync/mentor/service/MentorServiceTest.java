package com.skillsync.mentor.service;

import com.skillsync.mentor.dto.MentorApplicationRequest;
import com.skillsync.mentor.dto.MentorProfileResponse;
import com.skillsync.mentor.entity.MentorProfile;
import com.skillsync.mentor.enums.MentorStatus;
import com.skillsync.mentor.feign.AuthServiceClient;
import com.skillsync.mentor.repository.AvailabilitySlotRepository;
import com.skillsync.mentor.repository.MentorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private MentorService mentorService;

    private MentorProfile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = MentorProfile.builder()
                .id(1L)
                .userId(100L)
                .bio("Experienced developer")
                .experienceYears(5)
                .hourlyRate(BigDecimal.valueOf(50.0))
                .avgRating(0.0)
                .totalReviews(0)
                .totalSessions(0)
                .status(MentorStatus.PENDING)
                .skills(new ArrayList<>())
                .slots(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Apply as mentor - success")
    void apply_shouldCreateMentorProfile() {
        MentorApplicationRequest request = new MentorApplicationRequest("This is a very long bio because it needs to be at least 50 characters long so that validation passes in reality", 5, BigDecimal.valueOf(50.0), List.of(1L, 2L));
        when(mentorProfileRepository.existsByUserId(100L)).thenReturn(false);
        when(mentorProfileRepository.save(any(MentorProfile.class))).thenReturn(testProfile);

        MentorProfileResponse response = mentorService.apply(100L, request);

        assertNotNull(response);
        verify(mentorProfileRepository, atLeastOnce()).save(any(MentorProfile.class));
    }

    @Test
    @DisplayName("Apply as mentor - duplicate throws exception")
    void apply_shouldThrowForDuplicateApplication() {
        MentorApplicationRequest request = new MentorApplicationRequest("This is another very long bio because it needs to be at least 50 characters long for the application request", 5, BigDecimal.valueOf(50.0), List.of(1L));
        when(mentorProfileRepository.existsByUserId(100L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> mentorService.apply(100L, request));
    }

    @Test
    @DisplayName("Get mentor by ID - success")
    void getMentorById_shouldReturnProfile() {
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));

        MentorProfileResponse response = mentorService.getMentorById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Get mentor by ID - not found throws exception")
    void getMentorById_shouldThrowWhenNotFound() {
        when(mentorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> mentorService.getMentorById(999L));
    }

    @Test
    @DisplayName("Approve mentor - updates status and role")
    void approveMentor_shouldUpdateStatusAndPublishEvent() {
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(testProfile));
        when(mentorProfileRepository.save(any())).thenReturn(testProfile);

        mentorService.approveMentor(1L);

        assertEquals(MentorStatus.APPROVED, testProfile.getStatus());
        verify(authServiceClient).updateUserRole(100L, "ROLE_MENTOR");
        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
    }
}
