package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.repository.AvailabilitySlotRepository;
import com.skillsync.user.repository.MentorProfileRepository;
import com.skillsync.user.feign.AuthServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentorCommandServiceTest {

    @Mock private MentorProfileRepository mentorProfileRepository;
    @Mock private AvailabilitySlotRepository availabilitySlotRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;

    @InjectMocks private MentorCommandService mentorCommandService;

    @Test
    @DisplayName("updateAvgRating uses mentor profile id when available")
    void updateAvgRating_shouldUseProfileId_whenProfileExistsById() {
        MentorProfile profile = MentorProfile.builder()
                .id(10L)
                .userId(200L)
                .avgRating(0.0)
                .totalReviews(0)
                .build();

        when(mentorProfileRepository.findById(10L)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.save(profile)).thenReturn(profile);

        mentorCommandService.updateAvgRating(10L, 4.7, 15);

        assertEquals(4.7, profile.getAvgRating());
        assertEquals(15, profile.getTotalReviews());
        verify(mentorProfileRepository, never()).findByUserId(10L);
        verify(cacheService).evict(CacheService.vKey("user:mentor:10"));
        verify(cacheService).evict(CacheService.vKey("user:mentor:user:200"));
        verify(cacheService).evictByPattern(CacheService.vKey("user:mentor:search:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("user:mentor:pending:*"));
    }

    @Test
    @DisplayName("updateAvgRating falls back to mentor user id lookup")
    void updateAvgRating_shouldFallbackToUserId_whenProfileIdLookupMisses() {
        MentorProfile profile = MentorProfile.builder()
                .id(42L)
                .userId(501L)
                .avgRating(0.0)
                .totalReviews(0)
                .build();

        when(mentorProfileRepository.findById(501L)).thenReturn(Optional.empty());
        when(mentorProfileRepository.findByUserId(501L)).thenReturn(Optional.of(profile));
        when(mentorProfileRepository.save(profile)).thenReturn(profile);

        mentorCommandService.updateAvgRating(501L, 4.9, 23);

        assertEquals(4.9, profile.getAvgRating());
        assertEquals(23, profile.getTotalReviews());
        verify(cacheService).evict(CacheService.vKey("user:mentor:42"));
        verify(cacheService, never()).evict(CacheService.vKey("user:mentor:501"));
        verify(cacheService).evict(CacheService.vKey("user:mentor:user:501"));
        verify(cacheService).evictByPattern(CacheService.vKey("user:mentor:search:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("user:mentor:pending:*"));
    }
}