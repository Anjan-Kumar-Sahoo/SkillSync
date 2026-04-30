package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.entity.UserSkill;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService Unit Tests")
class UserCommandServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private CacheService cacheService;

    @InjectMocks private UserCommandService userCommandService;

    private static final Long USER_ID = 1L;
    private Profile existingProfile;

    @BeforeEach
    void setUp() {
        existingProfile = Profile.builder()
                .id(100L)
                .userId(USER_ID)
                .firstName("John")
                .lastName("Doe")
                .bio("Old Bio")
                .avatarUrl("old-url")
                .phone("111")
                .location("Old Loc")
                .profileCompletePct(100)
                .build();
    }

    @Nested
    @DisplayName("createOrUpdateProfile() tests")
    class CreateOrUpdateProfileTests {

        @Test
        @DisplayName("New Profile: Success path")
        void createProfile_New_Success() {
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Smith", "Bio", "url", "222", "Loc");
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

            ProfileResponse response = userCommandService.createOrUpdateProfile(USER_ID, request);

            assertThat(response.firstName()).isEqualTo("Jane");
            assertThat(response.profileCompletePct()).isEqualTo(100);
            
            ArgumentCaptor<Profile> captor = ArgumentCaptor.forClass(Profile.class);
            verify(profileRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Existing Profile: Update only non-null fields")
        void updateProfile_Partial_Success() {
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", null, null, "new-url", null, null);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
            when(profileRepository.save(any(Profile.class))).thenReturn(existingProfile);

            userCommandService.createOrUpdateProfile(USER_ID, request);

            assertThat(existingProfile.getFirstName()).isEqualTo("Jane");
            assertThat(existingProfile.getLastName()).isEqualTo("Doe"); // Unchanged
            assertThat(existingProfile.getBio()).isEqualTo("Old Bio"); // Unchanged
            assertThat(existingProfile.getAvatarUrl()).isEqualTo("new-url");
            
            verify(authServiceClient).updateUserName(USER_ID, "Jane", "Doe");
        }

        @Test
        @DisplayName("Field Branches: All null request fields leave profile unchanged")
        void updateProfile_AllNull_Unchanged() {
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, null, null, null);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
            when(profileRepository.save(any(Profile.class))).thenReturn(existingProfile);

            userCommandService.createOrUpdateProfile(USER_ID, request);

            assertThat(existingProfile.getFirstName()).isEqualTo("John");
            verify(authServiceClient, never()).updateUserName(anyLong(), anyString(), anyString());
        }

        @Test
        @DisplayName("Auth Sync: Falls back to AuthServiceClient when local names are null")
        void updateProfile_NameSync_Fallback() {
            existingProfile.setFirstName(null);
            existingProfile.setLastName(null);
            UpdateProfileRequest request = new UpdateProfileRequest("Jane", null, null, null, null, null);
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
            when(profileRepository.save(any(Profile.class))).thenReturn(existingProfile);
            when(authServiceClient.getUserById(USER_ID)).thenReturn(Map.of("lastName", "AuthLast"));

            userCommandService.createOrUpdateProfile(USER_ID, request);

            verify(authServiceClient).updateUserName(USER_ID, "Jane", "AuthLast");
        }

        @Test
        @DisplayName("Auth Sync: Handles null values from AuthServiceClient")
        void updateProfile_NameSync_AuthNulls() {
            existingProfile.setFirstName(null);
            existingProfile.setLastName(null);
            UpdateProfileRequest request = new UpdateProfileRequest(null, "Smith", null, null, null, null);
            
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existingProfile));
            when(profileRepository.save(any(Profile.class))).thenReturn(existingProfile);
            when(authServiceClient.getUserById(USER_ID)).thenReturn(Map.of()); // No names in auth

            userCommandService.createOrUpdateProfile(USER_ID, request);

            verify(authServiceClient).updateUserName(USER_ID, "", "Smith");
        }
    }

    @Nested
    @DisplayName("calculateCompleteness() tests")
    class CompletenessTests {

        @Test
        @DisplayName("Completeness: 0% (All null)")
        void calculateCompleteness_Zero() {
            Profile profile = Profile.builder().build();
            UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, null, null, null);
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenReturn(profile);

            ProfileResponse response = userCommandService.createOrUpdateProfile(USER_ID, request);
            assertThat(response.profileCompletePct()).isEqualTo(0);
        }

        @Test
        @DisplayName("Completeness: 100% (All present)")
        void calculateCompleteness_Full() {
            UpdateProfileRequest request = new UpdateProfileRequest("F", "L", "B", "A", "P", "L");
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(profileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            ProfileResponse response = userCommandService.createOrUpdateProfile(USER_ID, request);
            assertThat(response.profileCompletePct()).isEqualTo(100);
        }

        @Test
        @DisplayName("Completeness: Incremental field check")
        void calculateCompleteness_Incremental() {
            Profile profile = Profile.builder().build();
            
            // 20% - First Name
            profile.setFirstName("Exists");
            assertThat(invokeCompleteness(profile)).isEqualTo(20);
            
            // 40% - Last Name
            profile.setLastName("Exists");
            assertThat(invokeCompleteness(profile)).isEqualTo(40);
            
            // 60% - Bio
            profile.setBio("Exists");
            assertThat(invokeCompleteness(profile)).isEqualTo(60);
            
            // 80% - Phone
            profile.setPhone("Exists");
            assertThat(invokeCompleteness(profile)).isEqualTo(80);
            
            // 100% - Location
            profile.setLocation("Exists");
            assertThat(invokeCompleteness(profile)).isEqualTo(100);
        }

        private int invokeCompleteness(Profile profile) {
            // Helper to call createOrUpdate with partial profiles
            when(profileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
            when(profileRepository.save(any())).thenReturn(profile);
            return userCommandService.createOrUpdateProfile(USER_ID, new UpdateProfileRequest(null, null, null, null, null, null)).profileCompletePct();
        }
    }

    @Nested
    @DisplayName("Skill operations tests")
    class SkillTests {
        @Test
        @DisplayName("addSkill: Success")
        void addSkill_Success() {
            AddSkillRequest request = new AddSkillRequest(10L, "ADVANCED");
            when(userSkillRepository.existsByUserIdAndSkillId(USER_ID, 10L)).thenReturn(false);

            userCommandService.addSkill(USER_ID, request);

            verify(userSkillRepository).save(argThat(s -> s.getSkillId().equals(10L) && s.getProficiency() == UserSkill.Proficiency.ADVANCED));
            verify(cacheService).evict(anyString());
        }

        @Test
        @DisplayName("addSkill: Already exists throws exception")
        void addSkill_Duplicate_Throws() {
            when(userSkillRepository.existsByUserIdAndSkillId(USER_ID, 10L)).thenReturn(true);
            AddSkillRequest request = new AddSkillRequest(10L, "ADVANCED");

            assertThatThrownBy(() -> userCommandService.addSkill(USER_ID, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("already added");
        }

        @Test
        @DisplayName("removeSkill: Success")
        void removeSkill_Success() {
            userCommandService.removeSkill(USER_ID, 10L);
            verify(userSkillRepository).deleteByUserIdAndSkillId(USER_ID, 10L);
            verify(cacheService).evict(anyString());
        }
    }
}
