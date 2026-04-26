package com.skillsync.auth.security;

import com.skillsync.auth.entity.AuthUser;
import com.skillsync.auth.enums.Role;
import com.skillsync.auth.repository.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private AuthUserRepository authUserRepository;

    private UserDetailsServiceImpl userDetailsService;

    @BeforeEach
    void setUp() {
        userDetailsService = new UserDetailsServiceImpl(authUserRepository);
    }

    @Test
    void shouldLoadActiveUserWithRoleAuthority() {
        AuthUser user = AuthUser.builder()
                .email("active@skillsync.dev")
                .passwordHash("encoded")
                .role(Role.ROLE_LEARNER)
                .isActive(true)
                .build();
        when(authUserRepository.findByEmail("active@skillsync.dev")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("active@skillsync.dev");

        assertEquals("active@skillsync.dev", userDetails.getUsername());
        assertEquals("encoded", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> "ROLE_LEARNER".equals(a.getAuthority())));
    }

    @Test
    void shouldMarkDisabledWhenUserIsInactive() {
        AuthUser user = AuthUser.builder()
                .email("inactive@skillsync.dev")
                .passwordHash("encoded")
                .role(Role.ROLE_MENTOR)
                .isActive(false)
                .build();
        when(authUserRepository.findByEmail("inactive@skillsync.dev")).thenReturn(Optional.of(user));

        UserDetails userDetails = userDetailsService.loadUserByUsername("inactive@skillsync.dev");

        assertFalse(userDetails.isEnabled());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        when(authUserRepository.findByEmail("missing@skillsync.dev")).thenReturn(Optional.empty());

        UsernameNotFoundException ex = assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("missing@skillsync.dev")
        );

        assertTrue(ex.getMessage().contains("missing@skillsync.dev"));
    }
}
