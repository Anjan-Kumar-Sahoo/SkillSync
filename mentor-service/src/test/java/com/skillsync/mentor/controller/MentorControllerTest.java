package com.skillsync.mentor.controller;

import com.skillsync.mentor.dto.MentorProfileResponse;
import com.skillsync.mentor.service.MentorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MentorController.class)
class MentorControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private MentorService mentorService;

    @Test
    @DisplayName("GET /api/mentors/{id} - returns mentor")
    void getMentorById_shouldReturn200() throws Exception {
        MentorProfileResponse response = new MentorProfileResponse(1L, 100L, "John", "Doe", null,
                "Bio", 5, java.math.BigDecimal.valueOf(50.0), 4.5, 10, 20, "APPROVED", Collections.emptyList(), Collections.emptyList());
        when(mentorService.getMentorById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/mentors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Bio"));
    }

    @Test
    @DisplayName("PUT /api/mentors/{id}/approve - approves mentor")
    void approveMentor_shouldReturn200() throws Exception {
        mockMvc.perform(put("/api/mentors/1/approve"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /api/mentors/me - returns own profile")
    void getMyMentorProfile_shouldReturn200() throws Exception {
        MentorProfileResponse response = new MentorProfileResponse(1L, 100L, "John", "Doe", null,
                "Bio", 5, java.math.BigDecimal.valueOf(50.0), 0.0, 0, 0, "PENDING", Collections.emptyList(), Collections.emptyList());
        when(mentorService.getMentorByUserId(100L)).thenReturn(response);

        mockMvc.perform(get("/api/mentors/me").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }
}
