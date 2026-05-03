package com.skillsync.auth.service;

import com.skillsync.auth.dto.OAuthRequest;
import com.skillsync.auth.dto.RegisterRequest;
import com.skillsync.auth.dto.SetupPasswordRequest;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.entity.RefreshToken;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import com.skillsync.auth.repository.RefreshTokenRepository;
import com.skillsync.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceCoverageGapTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private com.skillsync.cache.CacheService cacheService;
    @InjectMocks private AuthService authService;

    private AuthUser user;

    @BeforeEach
    void setUp() {
        user = AuthUser.builder()
                .id(1L)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_LEARNER)
                .isVerified(true)
                .build();
    }

    @Test
    @DisplayName("register - Email Already Exists")
    void register_EmailExists() {
        when(authUserRepository.existsByEmail(anyString())).thenReturn(true);
        RegisterRequest request = new RegisterRequest("test@example.com", "Password@123", "John", "Doe");
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email already registered");
    }

    @Test
    @DisplayName("updateUserName - Null or Blank Inputs")
    void updateUserName_NullOrBlank() {
        when(authUserRepository.findById(1L)).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> authService.updateUserName(1L, null, "Doe"))
                .isInstanceOf(RuntimeException.class);
                
        assertThatThrownBy(() -> authService.updateUserName(1L, "John", null))
                .isInstanceOf(RuntimeException.class);
                
        assertThatThrownBy(() -> authService.updateUserName(1L, "   ", "Doe"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("loginWithOAuth - Existing User Linked Provider")
    void loginWithOAuth_ExistingUserLinkedProvider() {
        // User already has provider linked
        user.setProvider("google");
        user.setProviderId("g-123");
        
        when(authUserRepository.findByProviderAndProviderId("google", "g-123"))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
        
        OAuthRequest request = new OAuthRequest("test@example.com", "John", "Doe", "google", "g-123");
        authService.loginWithOAuth(request);
        
        // Should not save user again to link provider because it's already linked
        verify(authUserRepository, never()).save(any());
    }

    @Test
    @DisplayName("loginWithOAuth - Evict Oldest Refresh Token")
    void loginWithOAuth_EvictOldestRefreshToken() {
        when(authUserRepository.findByProviderAndProviderId("google", "g-123"))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
        
        List<RefreshToken> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tokens.add(RefreshToken.builder().id((long)i).build());
        }
        when(refreshTokenRepository.findByUserOrderByCreatedAtAsc(user)).thenReturn(tokens);
        
        OAuthRequest request = new OAuthRequest("test@example.com", "John", "Doe", "google", "g-123");
        authService.loginWithOAuth(request);
        
        verify(refreshTokenRepository).delete(tokens.get(0));
    }

    @Test
    @DisplayName("setupPassword - Null or Blank Password")
    void setupPassword_NullOrBlank() {
        user.setPasswordSet(false);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> authService.setupPassword(new SetupPasswordRequest("test@example.com", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password is mandatory");
                
        assertThatThrownBy(() -> authService.setupPassword(new SetupPasswordRequest("test@example.com", "   ")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Password is mandatory");
    }

    @Test
    @DisplayName("getUserCount - Blank Role")
    void getUserCount_BlankRole() {
        when(authUserRepository.count()).thenReturn(5L);
        long count = authService.getUserCount("   ");
        assertThat(count).isEqualTo(5L);
        verify(authUserRepository).count();
    }

    @Test
    @DisplayName("getAllUsersFiltered - Blank Role")
    void getAllUsersFiltered_BlankRole() {
        Page<AuthUser> page = new PageImpl<>(List.of(user));
        when(authUserRepository.findByFilters(eq(null), eq("search"), any(Pageable.class))).thenReturn(page);
        
        var result = authService.getAllUsersFiltered(0, 10, "   ", "search");
        assertThat(result.get("totalElements")).isEqualTo(1L);
    }

    @Test
    @DisplayName("completeRegistration - Not Verified")
    void completeRegistration_NotVerified() {
        user.setVerified(false);
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        com.skillsync.auth.dto.CompleteRegistrationRequest request = new com.skillsync.auth.dto.CompleteRegistrationRequest("test@example.com", "Password@123", "John", "Doe");
        assertThatThrownBy(() -> authService.completeRegistration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    @DisplayName("completeRegistration - Already Completed")
    void completeRegistration_AlreadyCompleted() {
        user.setVerified(true);
        user.setPasswordSet(true);
        user.setPasswordHash("encoded_pwd");
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        com.skillsync.auth.dto.CompleteRegistrationRequest request = new com.skillsync.auth.dto.CompleteRegistrationRequest("test@example.com", "Password@123", "John", "Doe");
        assertThatThrownBy(() -> authService.completeRegistration(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Registration already completed");
    }

    @Test
    @DisplayName("loginWithOAuth - New User")
    void loginWithOAuth_NewUser() {
        when(authUserRepository.findByProviderAndProviderId("google", "g-123")).thenReturn(Optional.empty());
        when(authUserRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        
        AuthUser newUser = AuthUser.builder().id(2L).email("new@example.com").role(Role.ROLE_LEARNER).build();
        when(authUserRepository.save(any(AuthUser.class))).thenReturn(newUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
        
        OAuthRequest request = new OAuthRequest("new@example.com", "Jane", "Doe", "google", "g-123");
        var response = authService.loginWithOAuth(request);
        
        assertThat(response.passwordSetupRequired()).isTrue();
    }

    @Test
    @DisplayName("loginWithOAuth - Existing User Unverified")
    void loginWithOAuth_ExistingUserUnverified() {
        user.setVerified(false);
        user.setProvider(null);
        when(authUserRepository.findByProviderAndProviderId("google", "g-123")).thenReturn(Optional.empty());
        when(authUserRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("token");
        when(jwtTokenProvider.generateRefreshToken(anyLong())).thenReturn("refresh");
        
        OAuthRequest request = new OAuthRequest("test@example.com", "John", "Doe", "google", "g-123");
        var response = authService.loginWithOAuth(request);
        
        assertThat(response.passwordSetupRequired()).isFalse();
        assertThat(user.isVerified()).isTrue();
        verify(authUserRepository).save(user);
    }

}
