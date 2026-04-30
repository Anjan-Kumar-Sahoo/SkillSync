package com.skillsync.user.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.user.dto.UpdateProfileRequest;
import com.skillsync.user.entity.Profile;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.repository.ProfileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserCommandService Surgical Coverage Tests")
class UserServiceCoverageTest {

    @Mock private ProfileRepository profileRepository;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private CacheService cacheService;
    
    @InjectMocks private UserCommandService userCommandService;

    @Test
    @DisplayName("createOrUpdateProfile: Fallback to Auth Service for names")
    void updateProfile_FallbackNames() {
        UpdateProfileRequest request = new UpdateProfileRequest("NewFirst", null, null, null, null, null);
        Profile profile = Profile.builder().userId(1L).firstName(null).lastName(null).build();
        
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenReturn(profile);
        
        // Mock AuthServiceClient to provide the missing lastName
        when(authServiceClient.getUserById(1L)).thenReturn(Map.of("firstName", "AuthFirst", "lastName", "AuthLast"));

        userCommandService.createOrUpdateProfile(1L, request);

        // Verify it picked up "AuthLast" from the fallback
        verify(authServiceClient).updateUserName(1L, "NewFirst", "AuthLast");
    }

    @Test
    @DisplayName("createOrUpdateProfile: Fallback handles null values from Auth Service")
    void updateProfile_FallbackAuthNull() {
        UpdateProfileRequest request = new UpdateProfileRequest("NewFirst", null, null, null, null, null);
        Profile profile = Profile.builder().userId(1L).firstName(null).lastName(null).build();
        
        when(profileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenReturn(profile);
        
        // Mock AuthServiceClient with null values
        when(authServiceClient.getUserById(1L)).thenReturn(Map.of());

        userCommandService.createOrUpdateProfile(1L, request);

        // Verify it used empty string for null fallback
        verify(authServiceClient).updateUserName(1L, "NewFirst", "");
    }
}
