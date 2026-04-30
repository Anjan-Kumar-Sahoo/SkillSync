package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.MentorProfileResponse;
import com.skillsync.user.dto.SkillSummary;
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
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MentorQueryService Surgical Coverage Tests")
class MentorQueryServiceCoverageTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private SessionServiceClient sessionServiceClient;
    @Mock private SkillServiceClient skillServiceClient;

    @InjectMocks private MentorQueryService mentorQueryService;

    @Test
    @DisplayName("enrichProfile: Handles Feign exceptions gracefully")
    void enrichProfile_Exceptions() {
        MentorProfile profile = MentorProfile.builder().userId(1L).status(MentorStatus.APPROVED).build();
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        
        // Mock getOrLoad to execute the loader (which is a Supplier)
        when(cacheService.getOrLoad(anyString(), any(), any(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<MentorProfileResponse> loader = inv.getArgument(3);
            return loader.get();
        });

        // Force exceptions in Feign clients
        when(authServiceClient.getUserById(anyLong())).thenThrow(new RuntimeException("Auth fail"));
        when(sessionServiceClient.getMentorMetrics(anyLong())).thenThrow(new RuntimeException("Session fail"));

        MentorProfileResponse result = mentorQueryService.getMentorByUserId(1L);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(1L);
        // Verify code reached catch blocks (implicitly by not crashing)
        verify(authServiceClient).getUserById(1L);
    }

    @Test
    @DisplayName("searchMentors: Handles empty skill resolution")
    void searchMentors_EmptySkills() {
        when(skillServiceClient.searchSkills("unknown")).thenReturn(List.of());

        var result = mentorQueryService.searchMentors("unknown", null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("resolveSkillIds: Handles SkillService exceptions")
    void resolveSkillIds_Exception() {
        when(skillServiceClient.searchSkills(anyString())).thenThrow(new RuntimeException("Skill fail"));

        // This is private but called via searchMentors
        var result = mentorQueryService.searchMentors("java", null, null, null, null, PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isZero();
        verify(skillServiceClient).searchSkills("java");
    }

    @Test
    @DisplayName("asInt/asDouble: Handles non-numeric values")
    void asNumeric_Fallbacks() {
        MentorProfile profile = MentorProfile.builder().userId(1L).status(MentorStatus.APPROVED).avgRating(4.5).totalReviews(10).build();
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        
        when(cacheService.getOrLoad(anyString(), any(), any(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            Supplier<MentorProfileResponse> loader = inv.getArgument(3);
            return loader.get();
        });

        // Mock session metrics with non-numeric values
        when(sessionServiceClient.getMentorMetrics(1L)).thenReturn(Map.of(
            "averageRating", "not-a-number",
            "totalReviews", "invalid"
        ));
        when(authServiceClient.getUserById(1L)).thenReturn(Map.of());

        MentorProfileResponse result = mentorQueryService.getMentorByUserId(1L);

        // Should fallback to original profile values
        assertThat(result.avgRating()).isEqualTo(4.5);
        assertThat(result.totalReviews()).isEqualTo(10);
    }
}
