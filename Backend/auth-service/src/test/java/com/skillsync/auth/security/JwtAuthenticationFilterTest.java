package com.skillsync.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void clearContextBefore() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void clearContextAfter() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAuthenticateUsingBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("learner@skillsync.dev")
                .password("encoded")
                .authorities("ROLE_LEARNER")
                .build();

        when(jwtTokenProvider.isTokenValid("valid-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("valid-token")).thenReturn("learner@skillsync.dev");
        when(userDetailsService.loadUserByUsername("learner@skillsync.dev")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertEquals("learner@skillsync.dev", principal.getUsername());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldAuthenticateUsingAccessTokenCookie() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("accessToken", "cookie-token"));
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        UserDetails userDetails = User.withUsername("mentor@skillsync.dev")
                .password("encoded")
                .authorities("ROLE_MENTOR")
                .build();

        when(jwtTokenProvider.isTokenValid("cookie-token")).thenReturn(true);
        when(jwtTokenProvider.extractEmail("cookie-token")).thenReturn("mentor@skillsync.dev");
        when(userDetailsService.loadUserByUsername("mentor@skillsync.dev")).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenTokenIsInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        when(jwtTokenProvider.isTokenValid("invalid-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void shouldSkipAuthenticationWhenNoTokenProvided() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(jwtTokenProvider, userDetailsService);
        verify(filterChain).doFilter(request, response);
    }
}
