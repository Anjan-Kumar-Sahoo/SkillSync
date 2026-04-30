package com.skillsync.auth.service;

import com.skillsync.auth.dto.RegisterRequest;
import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterEdgeCaseTest {
    @Mock AuthUserRepository authUserRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    @DisplayName("Register - null email throws exception")
    void register_nullEmail_throwsException() {
        RegisterRequest req = new RegisterRequest(null, "Password123!", "A", "B");
        assertThrows(NullPointerException.class, () -> authService.register(req));
    }

    @Test
    @DisplayName("Register - blank password throws exception")
    void register_blankPassword_throwsException() {
        RegisterRequest req = new RegisterRequest("a@b.com", "", "A", "B");
        when(authUserRepository.existsByEmail(anyString())).thenReturn(false);
        assertThrows(RuntimeException.class, () -> authService.register(req));
    }
}
