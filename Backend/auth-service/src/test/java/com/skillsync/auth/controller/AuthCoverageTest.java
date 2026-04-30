package com.skillsync.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.auth.dto.*;
import com.skillsync.auth.service.AuthService;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.security.UserDetailsServiceImpl;
import com.skillsync.auth.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Surgical Coverage Tests")
class AuthCoverageTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private OtpService otpService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("refreshToken: Get from body if cookie missing")
    void refreshToken_FromBody() throws Exception {
        RefreshTokenRequest body = new RefreshTokenRequest("body-token");
        AuthResponse response = new AuthResponse("at", "rt", 3600, "Bearer", null);
        
        when(authService.refreshToken(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        
        verify(authService).refreshToken(argThat(r -> r.refreshToken().equals("body-token")));
    }

    @Test
    @DisplayName("getCurrentUser: Invalid token path")
    void getCurrentUser_InvalidToken() throws Exception {
        when(jwtTokenProvider.isTokenValid(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("setupPassword: Unauthorized when no token")
    void setupPassword_NoToken() throws Exception {
        // password must be min 8 chars
        SetupPasswordRequest request = new SetupPasswordRequest("test@test.com", "password123");
        mockMvc.perform(post("/api/auth/setup-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cookie Logic: Localhost 127.0.0.1 branch")
    void cookieLogic_LocalhostIP() throws Exception {
        AuthResponse response = new AuthResponse("at", "rt", 3600, "Bearer", null);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-Host", "127.0.0.1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    @DisplayName("Cookie Logic: Production branch with Domain")
    void cookieLogic_Production() throws Exception {
        AuthResponse response = new AuthResponse("at", "rt", 3600, "Bearer", null);
        when(authService.login(any())).thenReturn(response);

        // Change serverName to NOT be localhost to trigger secure=true path
        mockMvc.perform(post("/api/auth/login")
                .with(request -> { request.setServerName("skillsync.com"); return request; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("Secure")));
    }

    @Test
    @DisplayName("Cookie Logic: IPv6 and Port normalization")
    void cookieLogic_IPv6AndPort() throws Exception {
        AuthResponse response = new AuthResponse("at", "rt", 3600, "Bearer", null);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .header("Origin", "http://[::1]:8080")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }

    @Test
    @DisplayName("Cookie Logic: Forwarded-Host multiple values")
    void cookieLogic_ForwardedHostList() throws Exception {
        AuthResponse response = new AuthResponse("at", "rt", 3600, "Bearer", null);
        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                .header("X-Forwarded-Host", "localhost, proxy.com")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"a@b.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
    }
}
