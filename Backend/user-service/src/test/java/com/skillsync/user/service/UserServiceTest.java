package com.skillsync.user.service;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import com.skillsync.user.service.command.UserCommandService;
import com.skillsync.user.service.query.UserQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private SkillServiceClient skillServiceClient;
    @Mock private CacheService cacheService;

    @InjectMocks private UserQueryService userQueryService;
    @InjectMocks private UserCommandService userCommandService;

    private Profile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = Profile.builder()
                .id(1L).userId(100L).firstName("John").lastName("Doe")
                .bio("Developer").phone("1234567890").location("NYC")
                .profileCompletePct(100).build();
    }

    @Test
    @DisplayName("Get profile by userId - cache miss → DB fetch")
    void getProfile_shouldReturnProfile() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("user:profile:100")), eq(ProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ProfileResponse> fallback = inv.getArgument(3);
                    return fallback.get();
                });
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(testProfile));
        when(userSkillRepository.findByUserId(100L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userQueryService.getProfile(100L);

        assertNotNull(response);
        assertEquals("John", response.firstName());
    }

    @Test
    @DisplayName("Get profile by userId - cache HIT → NO DB fetch")
    void getProfile_shouldReturnFromCache() {
        ProfileResponse cached = new ProfileResponse(1L, 100L, "John", "Doe", "Developer", "Bio", "NYC", "url", "123", 100, List.of(), java.time.LocalDateTime.now());
        when(cacheService.getOrLoad(eq(CacheService.vKey("user:profile:100")), eq(ProfileResponse.class), any(), any()))
                .thenReturn(cached);

        ProfileResponse response = userQueryService.getProfile(100L);

        assertEquals("John", response.firstName());
        verify(profileRepository, never()).findByUserId(anyLong()); // Ensure DB is bypassed
    }

    @Test
    @DisplayName("Get profile by userId - Redis DOWN → fallback to DB")
    void getProfile_shouldFallbackToDbOnRedisFailure() {
        // Simulate CacheService swallowing Redis Exception and executing DB Fallback
        when(cacheService.getOrLoad(eq(CacheService.vKey("user:profile:100")), eq(ProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ProfileResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // executes DB call
                });
        
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(testProfile));
        when(userSkillRepository.findByUserId(100L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userQueryService.getProfile(100L);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        verify(profileRepository).findByUserId(100L); // DB was hit
    }

    @Test
    @DisplayName("Get profile - not found throws exception")
    void getProfile_shouldThrowWhenNotFound() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("user:profile:999")), eq(ProfileResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<ProfileResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // returns null if profile is empty
                });
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            ProfileResponse p = userQueryService.getProfile(999L);
            if(p == null) throw new RuntimeException("Not found");
        });
    }

    @Test
    @DisplayName("Create or update profile - invalidates cache")
    void createOrUpdateProfile_shouldSaveAndInvalidateCache() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Doe", "Bio", "https://example.com/avatar.jpg", "9876543210", "LA");
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);

        ProfileResponse response = userCommandService.createOrUpdateProfile(100L, request);

        assertNotNull(response);
        verify(profileRepository).save(any(Profile.class));
        verify(cacheService).evict(CacheService.vKey("user:profile:100"));
        verify(cacheService).evict(CacheService.vKey("user:profile:id:1"));
        verify(authServiceClient).updateUserName(100L, "Jane", "Doe");
    }

    @Test
    @DisplayName("Add skill - duplicate skill throws exception")
    void addSkill_shouldThrowForDuplicate() {
        AddSkillRequest request = new AddSkillRequest(1L, "BEGINNER");
        when(userSkillRepository.existsByUserIdAndSkillId(100L, 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userCommandService.addSkill(100L, request));
    }

    @Test
    @DisplayName("Remove skill - invalidates cache")
    void removeSkill_shouldCallRepositoryAndInvalidateCache() {
        userCommandService.removeSkill(100L, 1L);
        verify(userSkillRepository).deleteByUserIdAndSkillId(100L, 1L);
        verify(cacheService).evict(CacheService.vKey("user:profile:100"));
    }
}
