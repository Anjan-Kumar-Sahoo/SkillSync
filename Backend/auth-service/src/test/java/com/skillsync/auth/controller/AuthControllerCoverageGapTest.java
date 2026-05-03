package com.skillsync.auth.controller;

import com.skillsync.auth.dto.SetupPasswordRequest;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.service.AuthService;
import com.skillsync.auth.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.http.Cookie;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerCoverageGapTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private OtpService otpService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.skillsync.auth.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("logout - Without refresh token cookie")
    void logout_WithoutCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("getCurrentUser - With invalid token")
    void getCurrentUser_InvalidToken() throws Exception {
        when(jwtTokenProvider.isTokenValid(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("setupPassword - Without token")
    void setupPassword_WithoutToken() throws Exception {
        mockMvc.perform(post("/api/auth/setup-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\", \"password\":\"Password@123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Cookie Helper - Localhost edge cases")
    void cookieHelper_LocalhostEdgeCases() throws Exception {
        // Test different localhost formats to hit isLocalHost branches
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Forwarded-Host", "127.0.0.1:8080"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/logout")
                        .header("Origin", "http://[::1]:3000"))
                .andExpect(status().isOk());

        // Invalid origin URI exception path
        mockMvc.perform(post("/api/auth/logout")
                        .header("Origin", "not-a-valid-uri^"))
                .andExpect(status().isOk());
                
        // Forwarded host with multiple proxies
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Forwarded-Host", "localhost, proxy.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Cookie Helper - Secure options")
    void cookieHelper_SecureOptions() throws Exception {
        // Not localhost, so secure = true
        mockMvc.perform(post("/api/auth/logout")
                        .header("X-Forwarded-Host", "example.com")
                        .header("Origin", "https://example.com"))
                .andExpect(status().isOk());
    }
}
