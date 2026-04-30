package com.skillsync.user.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.ProfileResponse;
import com.skillsync.user.dto.SkillSummary;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.entity.UserSkill;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserQueryService Unit Tests")
class UserQueryServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private SkillServiceClient skillServiceClient;
    @Mock private CacheService cacheService;

    @InjectMocks private UserQueryService userQueryService;

    private static final Long USER_ID = 1L;
    private Profile profile;

    @BeforeEach
    void setUp() {
        profile = Profile.builder()
                .id(100L)
                .userId(USER_ID)
                .firstName("John")
                .build();
        ReflectionTestUtils.setField(userQueryService, "profileTtl", 600L);
    }

    @Nested
    @DisplayName("getSkillsForUser() logic tests (via getProfile)")
    class GetSkillsForUserTests {

        @Test
        @DisplayName("Success: Fetches all skills from Feign")
        void getSkillsForUser_Success() {
            // Arrange
            when(cacheService.getOrLoad(anyString(), eq(ProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(i -> ((Supplier<ProfileResponse>) i.getArgument(3)).get());
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(userSkillRepository.findByUserId(USER_ID)).thenReturn(List.of(
                    UserSkill.builder().skillId(10L).build(),
                    UserSkill.builder().skillId(20L).build()
            ));
            when(skillServiceClient.getSkillById(10L)).thenReturn(new SkillSummary(10L, "Java", "Backend"));
            when(skillServiceClient.getSkillById(20L)).thenReturn(new SkillSummary(20L, "Spring", "Framework"));

            // Act
            ProfileResponse response = userQueryService.getProfile(USER_ID);

            // Assert
            assertThat(response.skills()).hasSize(2);
            assertThat(response.skills().get(0).name()).isEqualTo("Java");
            assertThat(response.skills().get(1).name()).isEqualTo("Spring");
        }

        @Test
        @DisplayName("Exception: Handles Feign failure gracefully")
        void getSkillsForUser_FeignFailure_ReturnsUnknown() {
            // Arrange
            when(cacheService.getOrLoad(anyString(), eq(ProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(i -> ((Supplier<ProfileResponse>) i.getArgument(3)).get());
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(userSkillRepository.findByUserId(USER_ID)).thenReturn(List.of(
                    UserSkill.builder().skillId(10L).build()
            ));
            when(skillServiceClient.getSkillById(10L)).thenThrow(new RuntimeException("Feign Error"));

            // Act
            ProfileResponse response = userQueryService.getProfile(USER_ID);

            // Assert
            assertThat(response.skills()).hasSize(1);
            assertThat(response.skills().get(0).name()).isEqualTo("Unknown");
            assertThat(response.skills().get(0).id()).isEqualTo(10L);
        }

        @Test
        @DisplayName("Empty: Handles user with no skills")
        void getSkillsForUser_Empty() {
            // Arrange
            when(cacheService.getOrLoad(anyString(), eq(ProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(i -> ((Supplier<ProfileResponse>) i.getArgument(3)).get());
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(userSkillRepository.findByUserId(USER_ID)).thenReturn(List.of());

            // Act
            ProfileResponse response = userQueryService.getProfile(USER_ID);

            // Assert
            assertThat(response.skills()).isEmpty();
            verify(skillServiceClient, never()).getSkillById(anyLong());
        }
    }

    @Nested
    @DisplayName("Cache-aside logic tests")
    class CacheTests {
        @Test
        @DisplayName("getProfile: Returns null if profile not in DB (Null sentinel)")
        void getProfile_NotFound() {
            when(cacheService.getOrLoad(anyString(), eq(ProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(i -> ((Supplier<ProfileResponse>) i.getArgument(3)).get());
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            ProfileResponse response = userQueryService.getProfile(USER_ID);

            assertThat(response).isNull();
        }

        @Test
        @DisplayName("getProfileById: Success path")
        void getProfileById_Success() {
            when(cacheService.getOrLoad(anyString(), eq(ProfileResponse.class), any(Duration.class), any()))
                    .thenAnswer(i -> ((Supplier<ProfileResponse>) i.getArgument(3)).get());
            
            when(profileRepository.findById(100L)).thenReturn(Optional.of(profile));
            when(userSkillRepository.findByUserId(USER_ID)).thenReturn(List.of());

            ProfileResponse response = userQueryService.getProfileById(100L);

            assertThat(response.userId()).isEqualTo(USER_ID);
        }
    }
}
