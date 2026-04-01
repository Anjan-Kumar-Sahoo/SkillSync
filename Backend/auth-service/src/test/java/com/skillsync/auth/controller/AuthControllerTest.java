package com.skillsync.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.auth.dto.*;
import com.skillsync.auth.service.AuthService;
import com.skillsync.auth.service.OtpService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.security.UserDetailsServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private OtpService otpService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - 201 Created")
    void register_shouldReturn201() throws Exception {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "John", "Doe");
        AuthResponse response = new AuthResponse("token", "refresh", 3600, "Bearer",
                new UserSummary(1L, "test@example.com", "ROLE_LEARNER", "John", "Doe"));

        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(jsonPath("$.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/auth/login - 200 OK")
    void login_shouldReturn200() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        AuthResponse response = new AuthResponse("token", "refresh", 3600, "Bearer",
                new UserSummary(1L, "test@example.com", "ROLE_LEARNER", "John", "Doe"));

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    @DisplayName("POST /api/auth/register - 400 for invalid email")
    void register_shouldReturn400ForInvalidInput() throws Exception {
        String invalidJson = """
                {"email":"invalid","password":"short","firstName":"","lastName":""}
                """;

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/verify-otp - 200 OK")
    void verifyOtp_shouldReturn200() throws Exception {
        VerifyOtpRequest request = new VerifyOtpRequest("test@example.com", "123456");

        mockMvc.perform(post("/api/auth/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email verified successfully"));
    }
}
