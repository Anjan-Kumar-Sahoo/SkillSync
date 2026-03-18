package com.skillsync.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.user.dto.*;
import com.skillsync.user.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private UserService userService;

    @Test
    @DisplayName("GET /api/users/me - returns profile")
    void getMyProfile_shouldReturn200() throws Exception {
        ProfileResponse response = new ProfileResponse(1L, 100L, "John", "Doe", null, "Bio", null, "123", "NYC", 100, Collections.emptyList(), LocalDateTime.now());
        when(userService.getProfile(100L)).thenReturn(response);

        mockMvc.perform(get("/api/users/me").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("PUT /api/users/me - updates profile")
    void updateMyProfile_shouldReturn200() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest("Jane", "Doe", "Bio", "123", "LA");
        ProfileResponse response = new ProfileResponse(1L, 100L, "Jane", "Doe", null, "Bio", null, "123", "LA", 100, Collections.emptyList(), LocalDateTime.now());
        when(userService.createOrUpdateProfile(eq(100L), any())).thenReturn(response);

        mockMvc.perform(put("/api/users/me")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @DisplayName("GET /api/users/{id} - returns profile by ID")
    void getProfileById_shouldReturn200() throws Exception {
        ProfileResponse response = new ProfileResponse(1L, 100L, "John", "Doe", null, "Bio", null, "123", "NYC", 100, Collections.emptyList(), LocalDateTime.now());
        when(userService.getProfileById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(100));
    }
}
