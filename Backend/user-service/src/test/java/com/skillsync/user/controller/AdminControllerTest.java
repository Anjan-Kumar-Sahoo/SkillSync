package com.skillsync.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.user.dto.MentorProfileResponse;
import com.skillsync.user.entity.MentorProfile;
import com.skillsync.user.enums.MentorStatus;
import com.skillsync.user.feign.AuthServiceClient;
import com.skillsync.user.feign.SessionServiceClient;
import com.skillsync.user.repository.MentorProfileRepository;
import com.skillsync.user.service.command.MentorCommandService;
import com.skillsync.user.service.query.MentorQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthServiceClient authServiceClient;
    @MockBean private SessionServiceClient sessionServiceClient;
    @MockBean private MentorProfileRepository mentorProfileRepository;
    @MockBean private MentorQueryService mentorQueryService;
    @MockBean private MentorCommandService mentorCommandService;

    // ─── GET /api/admin/stats ───

    @Test
    @DisplayName("GET /stats — returns aggregated stats")
    void getStats_shouldReturnStats() throws Exception {
        when(authServiceClient.getUserCount(null)).thenReturn(50L);
        when(sessionServiceClient.getSessionCount()).thenReturn(20L);
        when(mentorProfileRepository.countByStatus(MentorStatus.APPROVED)).thenReturn(10L);
        when(mentorProfileRepository.countByStatus(MentorStatus.PENDING)).thenReturn(3L);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(50))
                .andExpect(jsonPath("$.totalMentors").value(10))
                .andExpect(jsonPath("$.totalSessions").value(20))
                .andExpect(jsonPath("$.pendingMentorApprovals").value(3));
    }

    @Test
    @DisplayName("GET /stats — handles feign failures gracefully")
    void getStats_shouldHandleFeignFailures() throws Exception {
        when(authServiceClient.getUserCount(null)).thenThrow(new RuntimeException("auth down"));
        when(sessionServiceClient.getSessionCount()).thenThrow(new RuntimeException("session down"));
        when(mentorProfileRepository.countByStatus(MentorStatus.APPROVED)).thenReturn(5L);
        when(mentorProfileRepository.countByStatus(MentorStatus.PENDING)).thenReturn(1L);

        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(0))
                .andExpect(jsonPath("$.totalSessions").value(0))
                .andExpect(jsonPath("$.totalMentors").value(5));
    }

    // ─── GET /api/admin/users ───

    @Test
    @DisplayName("GET /users — returns user list from auth-service")
    void getAllUsers_shouldReturnUsers() throws Exception {
        Map<String, Object> usersPage = Map.of("content", List.of(), "totalElements", 0);
        when(authServiceClient.getAllUsers(0, 100, null, null)).thenReturn(usersPage);

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /users — handles auth-service failure")
    void getAllUsers_shouldHandleAuthServiceFailure() throws Exception {
        when(authServiceClient.getAllUsers(anyInt(), anyInt(), any(), any()))
                .thenThrow(new RuntimeException("auth down"));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // ─── PUT /api/admin/users/{id}/role ───

    @Test
    @DisplayName("PUT /users/{id}/role — promote to mentor")
    void updateUserRole_shouldPromoteToMentor() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_MENTOR"))))
                .andExpect(status().isOk());

        verify(mentorCommandService).promoteUserToMentor(1L);
    }

    @Test
    @DisplayName("PUT /users/{id}/role — demote to learner")
    void updateUserRole_shouldDemoteToLearner() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_LEARNER"))))
                .andExpect(status().isOk());

        verify(mentorCommandService).demoteUserToLearner(eq(1L), anyString());
    }

    @Test
    @DisplayName("PUT /users/{id}/role — general role update via auth-service")
    void updateUserRole_shouldDelegateToAuthService() throws Exception {
        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "ROLE_ADMIN"))))
                .andExpect(status().isOk());

        verify(authServiceClient).updateUserRole(1L, "ROLE_ADMIN");
    }

    // ─── DELETE /api/admin/users/{id} ───

    @Test
    @DisplayName("DELETE /users/{id} — cleans mentor data and deletes user")
    void deleteUser_shouldCleanMentorAndDeleteUser() throws Exception {
        MentorProfile mockProfile = MentorProfile.builder().id(1L).userId(1L).build();
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isOk());

        verify(mentorProfileRepository).delete(mockProfile);
        verify(authServiceClient).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /users/{id} — returns 500 if auth-service deletion fails")
    void deleteUser_shouldReturn500OnAuthFailure() throws Exception {
        when(mentorProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("auth down")).when(authServiceClient).deleteUser(1L);

        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isInternalServerError());
    }

    // ─── GET /api/admin/mentors/pending ───

    @Test
    @DisplayName("GET /mentors/pending — returns pending mentors page")
    void getPendingMentors_shouldReturnPage() throws Exception {
        when(mentorQueryService.getPendingApplications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/admin/mentors/pending"))
                .andExpect(status().isOk());
    }

    // ─── POST /api/admin/mentors/{id}/approve ───

    @Test
    @DisplayName("POST /mentors/{id}/approve — delegates to command service")
    void approveMentor_shouldDelegate() throws Exception {
        mockMvc.perform(post("/api/admin/mentors/1/approve"))
                .andExpect(status().isOk());

        verify(mentorCommandService).approveMentor(1L);
    }

    // ─── POST /api/admin/mentors/{id}/reject ───

    @Test
    @DisplayName("POST /mentors/{id}/reject — delivers rejection reason")
    void rejectMentor_shouldDeliver() throws Exception {
        mockMvc.perform(post("/api/admin/mentors/1/reject")
                        .param("reason", "Insufficient experience"))
                .andExpect(status().isOk());

        verify(mentorCommandService).rejectMentor(1L, "Insufficient experience");
    }

    @Test
    @DisplayName("POST /mentors/{id}/reject — uses default reason when none provided")
    void rejectMentor_shouldUseDefaultReason() throws Exception {
        mockMvc.perform(post("/api/admin/mentors/1/reject"))
                .andExpect(status().isOk());

        verify(mentorCommandService).rejectMentor(1L, "Rejected by admin");
    }
}
