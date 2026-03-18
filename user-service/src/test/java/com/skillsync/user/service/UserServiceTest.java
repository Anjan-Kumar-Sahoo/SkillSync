package com.skillsync.user.service;

import com.skillsync.user.dto.*;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.feign.SkillServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import com.skillsync.user.repository.UserSkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private UserSkillRepository userSkillRepository;
    @Mock private SkillServiceClient skillServiceClient;

    @InjectMocks private UserService userService;

    private Profile testProfile;

    @BeforeEach
    void setUp() {
        testProfile = Profile.builder()
                .id(1L)
                .userId(100L)
                .firstName("John")
                .lastName("Doe")
                .bio("Developer")
                .phone("1234567890")
                .location("NYC")
                .profileCompletePct(100)
                .build();
    }

    @Test
    @DisplayName("Get profile by userId - success")
    void getProfile_shouldReturnProfile() {
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(testProfile));
        when(userSkillRepository.findByUserId(100L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userService.getProfile(100L);

        assertNotNull(response);
        assertEquals("John", response.firstName());
        assertEquals(100L, response.userId());
    }

    @Test
    @DisplayName("Get profile - not found throws exception")
    void getProfile_shouldThrowWhenNotFound() {
        when(profileRepository.findByUserId(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getProfile(999L));
    }

    @Test
    @DisplayName("Create or update profile - success")
    void createOrUpdateProfile_shouldSaveProfile() {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Doe", "Bio", "9876543210", "LA");
        when(profileRepository.findByUserId(100L)).thenReturn(Optional.of(testProfile));
        when(profileRepository.save(any(Profile.class))).thenReturn(testProfile);
        when(userSkillRepository.findByUserId(100L)).thenReturn(Collections.emptyList());

        ProfileResponse response = userService.createOrUpdateProfile(100L, request);

        assertNotNull(response);
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    @DisplayName("Add skill - duplicate skill throws exception")
    void addSkill_shouldThrowForDuplicate() {
        AddSkillRequest request = new AddSkillRequest(1L, "BEGINNER");
        when(userSkillRepository.existsByUserIdAndSkillId(100L, 1L)).thenReturn(true);

        assertThrows(RuntimeException.class, () -> userService.addSkill(100L, request));
    }

    @Test
    @DisplayName("Remove skill - success")
    void removeSkill_shouldCallRepository() {
        userService.removeSkill(100L, 1L);
        verify(userSkillRepository).deleteByUserIdAndSkillId(100L, 1L);
    }
}
