package com.skillsync.session.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillsync.session.dto.*;
import com.skillsync.session.service.command.ReviewCommandService;
import com.skillsync.session.service.query.ReviewQueryService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private ReviewCommandService reviewCommandService;
    @MockBean private ReviewQueryService reviewQueryService;

    private final ReviewResponse sampleReview = new ReviewResponse(
            1L, 1L, 100L, 200L, 5, "Great mentor!", LocalDateTime.now());

    @Test
    @DisplayName("GET /mentor/{mentorId} — returns paginated reviews")
    void getMentorReviews_shouldReturnPage() throws Exception {
        when(reviewQueryService.getMentorReviews(eq(100L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleReview)));

        mockMvc.perform(get("/api/reviews/mentor/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    @DisplayName("GET /mentor/{mentorId}/summary — returns rating summary")
    void getMentorRating_shouldReturnSummary() throws Exception {
        MentorRatingSummary summary = new MentorRatingSummary(
                100L, 4.5, 10, 20L, 2L, false, Map.of(5, 7, 4, 3));
        when(reviewQueryService.getMentorRatingSummary(100L)).thenReturn(summary);

        mockMvc.perform(get("/api/reviews/mentor/100/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.5))
                .andExpect(jsonPath("$.totalReviews").value(10));
    }

    @Test
    @DisplayName("GET /{id} — returns single review")
    void getReview_shouldReturnReview() throws Exception {
        when(reviewQueryService.getReviewById(1L)).thenReturn(sampleReview);

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comment").value("Great mentor!"));
    }

    @Test
    @DisplayName("GET /me — returns user's reviews")
    void getMyReviews_shouldReturnPage() throws Exception {
        when(reviewQueryService.getMyReviews(eq(200L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleReview)));

        mockMvc.perform(get("/api/reviews/me").header("X-User-Id", "200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].reviewerId").value(200));
    }

    @Test
    @DisplayName("POST / — submits review (201)")
    void submitReview_shouldSubmitAndReturn201() throws Exception {
        ReviewRequest request = new ReviewRequest(1L, 100L, 5, "Great!");
        when(reviewCommandService.submitReview(eq(200L), any(ReviewRequest.class)))
                .thenReturn(sampleReview);

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("DELETE /{id} — deletes review")
    void deleteReview_shouldDelete() throws Exception {
        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isOk());

        verify(reviewCommandService).deleteReview(1L);
    }
}
