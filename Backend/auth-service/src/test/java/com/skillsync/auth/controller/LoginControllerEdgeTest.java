package com.skillsync.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.auth.dto.LoginRequest;
import com.skillsync.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.skillsync.auth.service.OtpService;
import com.skillsync.auth.security.JwtTokenProvider;
import com.skillsync.auth.security.UserDetailsServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerEdgeTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean OtpService otpService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /api/auth/login - 400 for missing email")
    void login_missingEmail_returns400() throws Exception {
        LoginRequest req = new LoginRequest(null, "Password123!");
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
