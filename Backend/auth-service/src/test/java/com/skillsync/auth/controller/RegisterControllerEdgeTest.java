package com.skillsync.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.auth.dto.RegisterRequest;
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
class RegisterControllerEdgeTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AuthService authService;
    @MockBean OtpService otpService;
    @MockBean JwtTokenProvider jwtTokenProvider;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("POST /api/auth/register - 400 on service error")
    void register_serviceError_returns400() throws Exception {
        RegisterRequest req = new RegisterRequest("fail@x.com", "Password123!", "AA", "BB");
        when(authService.register(any(RegisterRequest.class))).thenThrow(new RuntimeException("fail"));
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
