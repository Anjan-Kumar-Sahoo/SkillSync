package com.skillsync.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.user.dto.*;
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

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MentorController.class)
@AutoConfigureMockMvc(addFilters = false)
class MentorControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private MentorCommandService mentorCommandService;
    @MockBean private MentorQueryService mentorQueryService;

    private final MentorProfileResponse sampleMentor = new MentorProfileResponse(
            1L, 100L, "John", "Doe", "john@test.com", null,
            "Expert Java developer", 5, BigDecimal.valueOf(50), 4.5, 10, 20,
            "APPROVED", List.of(), List.of());

    // ─── QUERY ENDPOINTS ───

    @Test
    @DisplayName("GET /search — returns paginated mentors")
    void searchMentors_shouldReturnPage() throws Exception {
        when(mentorQueryService.searchMentors(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleMentor)));

        mockMvc.perform(get("/api/mentors/search")
                        .param("skill", "Java")
                        .param("rating", "4.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].firstName").value("John"));
    }

    @Test
    @DisplayName("GET /{id} — returns mentor by ID")
    void getMentorById_shouldReturnMentor() throws Exception {
        when(mentorQueryService.getMentorById(1L)).thenReturn(sampleMentor);

        mockMvc.perform(get("/api/mentors/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("John"));
    }

    @Test
    @DisplayName("GET /me — returns current user's mentor profile")
    void getMyMentorProfile_shouldReturnProfile() throws Exception {
        when(mentorQueryService.getMentorByUserId(100L)).thenReturn(sampleMentor);

        mockMvc.perform(get("/api/mentors/me").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(100));
    }

    @Test
    @DisplayName("GET /pending — returns pending applications page")
    void getPendingApplications_shouldReturnPage() throws Exception {
        when(mentorQueryService.getPendingApplications(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/mentors/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /me/availability — returns availability slots")
    void getMyAvailability_shouldReturnSlots() throws Exception {
        MentorProfileResponse withAvailability = new MentorProfileResponse(
                1L, 100L, "John", "Doe", "john@test.com", null,
                "Bio", 5, BigDecimal.valueOf(50), 4.5, 10, 20,
                "APPROVED", List.of(), List.of());
        when(mentorQueryService.getMentorByUserId(100L)).thenReturn(withAvailability);

        mockMvc.perform(get("/api/mentors/me/availability").header("X-User-Id", "100"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /me/availability — returns empty list if no mentor profile")
    void getMyAvailability_shouldReturnEmptyWhenNoProfile() throws Exception {
        when(mentorQueryService.getMentorByUserId(100L)).thenReturn(null);

        mockMvc.perform(get("/api/mentors/me/availability").header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ─── COMMAND ENDPOINTS ───

    @Test
    @DisplayName("POST /apply — submits mentor application (202)")
    void apply_shouldSubmitApplication() throws Exception {
        MentorApplicationRequest request = new MentorApplicationRequest(
                "I have over five years of focused mentoring and production Java experience.",
                5,
                BigDecimal.valueOf(50),
                List.of(1L));
        when(mentorCommandService.apply(eq(100L), any(MentorApplicationRequest.class)))
                .thenReturn(sampleMentor);

        mockMvc.perform(post("/api/mentors/apply")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("POST /me/availability — adds availability slot")
    void addAvailability_shouldAddSlot() throws Exception {
                AvailabilitySlotRequest slotReq = new AvailabilitySlotRequest(1, LocalTime.of(9, 0), LocalTime.of(17, 0));
                AvailabilitySlotResponse slotResp = new AvailabilitySlotResponse(1L, 1, LocalTime.of(9, 0), LocalTime.of(17, 0), true);
        when(mentorCommandService.addAvailability(eq(100L), any(AvailabilitySlotRequest.class)))
                .thenReturn(slotResp);

        mockMvc.perform(post("/api/mentors/me/availability")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /me/availability/{id} — removes availability slot")
    void removeAvailability_shouldRemove() throws Exception {
        mockMvc.perform(delete("/api/mentors/me/availability/1"))
                .andExpect(status().isOk());

        verify(mentorCommandService).removeAvailability(1L);
    }

    @Test
    @DisplayName("PUT /{id}/approve — approves mentor")
    void approveMentor_shouldApprove() throws Exception {
        mockMvc.perform(put("/api/mentors/1/approve"))
                .andExpect(status().isOk());

        verify(mentorCommandService).approveMentor(1L);
    }

    @Test
    @DisplayName("PUT /{id}/reject — rejects mentor with reason")
    void rejectMentor_shouldReject() throws Exception {
        mockMvc.perform(put("/api/mentors/1/reject").param("reason", "Need more experience"))
                .andExpect(status().isOk());

        verify(mentorCommandService).rejectMentor(1L, "Need more experience");
    }
}
