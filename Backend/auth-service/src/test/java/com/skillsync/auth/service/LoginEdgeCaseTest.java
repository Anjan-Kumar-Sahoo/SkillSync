package com.skillsync.auth.service;

import com.skillsync.auth.dto.LoginRequest;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginEdgeCaseTest {
    @Mock AuthUserRepository authUserRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("Login - null email throws exception")
    void login_nullEmail_throwsException() {
        LoginRequest req = new LoginRequest(null, "Password123!");
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }

    @Test
    @DisplayName("Login - user not found throws exception")
    void login_userNotFound_throwsException() {
        LoginRequest req = new LoginRequest("notfound@x.com", "Password123!");
        when(authUserRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.login(req));
    }
}
