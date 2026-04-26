package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.feign.SessionServiceClient;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.MentorProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MentorQueryServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private SessionServiceClient sessionServiceClient;
    @Mock private SkillServiceClient skillServiceClient;

    @InjectMocks private MentorQueryService mentorQueryService;

    private MentorProfile buildTestProfile() {
        return MentorProfile.builder()
                .id(1L).userId(100L).bio("Expert")
                .experienceYears(5).hourlyRate(BigDecimal.valueOf(50))
                .avgRating(4.5).totalReviews(10).totalSessions(20)
                .status(MentorStatus.APPROVED).build();
    }

    @Test
    @DisplayName("getMentorById — cache miss loads from DB and enriches")
    void getMentorById_shouldLoadFromDb() {
        MentorProfile profile = buildTestProfile();
        when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorProfileResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(authServiceClient.getUserById(100L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "email", "john@test.com", "avatarUrl", "url"));
        when(sessionServiceClient.getMentorMetrics(100L)).thenReturn(
                Map.of("averageRating", 4.7, "totalReviews", 15, "completedSessions", 25));

        MentorProfileResponse result = mentorQueryService.getMentorById(1L);

        assertNotNull(result);
        assertEquals("John", result.firstName());
        assertEquals(4.7, result.avgRating());
    }

    @Test
    @DisplayName("getMentorById — returns null for non-existent ID")
    void getMentorById_shouldReturnNullWhenNotFound() {
        when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorProfileResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        MentorProfileResponse result = mentorQueryService.getMentorById(999L);

        assertNull(result);
    }

    @Test
    @DisplayName("getMentorByUserId — cache miss loads from DB")
    void getMentorByUserId_shouldLoadFromDb() {
        MentorProfile profile = buildTestProfile();
        when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorProfileResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorProfileRepository.findByUserId(100L)).thenReturn(Optional.of(profile));
        when(authServiceClient.getUserById(100L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "email", "john@test.com"));
        when(sessionServiceClient.getMentorMetrics(100L)).thenReturn(Map.of());

        MentorProfileResponse result = mentorQueryService.getMentorByUserId(100L);

        assertNotNull(result);
    }

    @Test
    @DisplayName("getPendingApplications — returns pending mentors page")
    void getPendingApplications_shouldReturnPage() {
        MentorProfile profile = buildTestProfile();
        profile.setStatus(MentorStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);
        when(mentorProfileRepository.findByStatus(MentorStatus.PENDING, pageable))
                .thenReturn(new PageImpl<>(List.of(profile)));
        when(authServiceClient.getUserById(100L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "email", "john@test.com"));
        when(sessionServiceClient.getMentorMetrics(100L)).thenReturn(Map.of());

        var result = mentorQueryService.getPendingApplications(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("enrichProfile — handles auth-service failure gracefully")
    void enrichProfile_shouldHandleAuthFailure() {
        MentorProfile profile = buildTestProfile();
        when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorProfileResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(authServiceClient.getUserById(100L)).thenThrow(new RuntimeException("auth down"));

        MentorProfileResponse result = mentorQueryService.getMentorById(1L);

        // Should still return, just without enriched user details
        assertNotNull(result);
    }

    @Test
    @DisplayName("enrichProfile — handles session-service failure gracefully")
    void enrichProfile_shouldHandleSessionFailure() {
        MentorProfile profile = buildTestProfile();
        when(cacheService.getOrLoad(anyString(), eq(MentorProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<MentorProfileResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(mentorProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(authServiceClient.getUserById(100L)).thenReturn(
                Map.of("firstName", "John", "lastName", "Doe", "email", "john@test.com"));
        when(sessionServiceClient.getMentorMetrics(100L))
                .thenThrow(new RuntimeException("session down"));

        MentorProfileResponse result = mentorQueryService.getMentorById(1L);

        assertNotNull(result);
        assertEquals("John", result.firstName());
    }
}
