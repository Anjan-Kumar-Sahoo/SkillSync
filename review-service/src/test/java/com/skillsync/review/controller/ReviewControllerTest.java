package com.skillsync.review.controller;

import com.skillsync.review.dto.ReviewResponse;
import com.skillsync.review.service.ReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ReviewService reviewService;

    @Test
    @DisplayName("GET /api/reviews/{id} - returns review")
    void getReview_shouldReturn200() throws Exception {
        ReviewResponse response = new ReviewResponse(1L, 10L, 2L, 3L, 5, "Great!", LocalDateTime.now());
        when(reviewService.getReviewById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    @DisplayName("DELETE /api/reviews/{id} - deletes review")
    void deleteReview_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/reviews/1"))
                .andExpect(status().isOk());
    }
}
