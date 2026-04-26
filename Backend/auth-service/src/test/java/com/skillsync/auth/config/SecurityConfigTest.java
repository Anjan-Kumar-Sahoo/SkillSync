package com.skillsync.auth.config;

import com.skillsync.auth.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityConfigTest {

    @Mock
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Mock
    private AuthenticationConfiguration authenticationConfiguration;

    @Mock
    private AuthenticationManager authenticationManager;

    private SecurityConfig securityConfig;

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(jwtAuthenticationFilter);
    }

    @Test
    void shouldCreateBcryptPasswordEncoder() {
        PasswordEncoder encoder = securityConfig.passwordEncoder();

        String encoded = encoder.encode("plain-password");
        assertTrue(encoder.matches("plain-password", encoded));
    }

    @Test
    void shouldDelegateAuthenticationManagerCreationToAuthenticationConfiguration() throws Exception {
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        AuthenticationManager resolved = securityConfig.authenticationManager(authenticationConfiguration);

        assertSame(authenticationManager, resolved);
    }
}
