package com.skillsync.session.mapper;

import com.skillsync.session.dto.ReviewResponse;
import com.skillsync.session.dto.SessionResponse;
import com.skillsync.session.entity.Review;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Session/Review Mapper classes.
 */
class MapperTest {

    @Nested
    @DisplayName("SessionMapper")
    class SessionMapperTests {

        @Test
        @DisplayName("should map Session entity to SessionResponse")
        void mapSession() {
            LocalDateTime now = LocalDateTime.now();
            Session session = Session.builder()
                    .id(1L).mentorId(10L).learnerId(20L)
                    .topic("Java Basics").description("Intro session")
                    .sessionDate(now).durationMinutes(60)
                    .meetingLink("https://meet.google.com/abc")
                    .status(SessionStatus.ACCEPTED)
                    .build();

            SessionResponse response = SessionMapper.toResponse(session);

            assertEquals(1L, response.id());
            assertEquals(10L, response.mentorId());
            assertEquals(20L, response.learnerId());
            assertEquals("Java Basics", response.topic());
            assertEquals("Intro session", response.description());
            assertEquals(60, response.durationMinutes());
            assertEquals("ACCEPTED", response.status());
            assertEquals("https://meet.google.com/abc", response.meetingLink());
        }

        @Test
        @DisplayName("should handle null cancel reason")
        void mapSession_nullCancelReason() {
            Session session = Session.builder()
                    .id(2L).mentorId(10L).learnerId(20L)
                    .topic("Test").status(SessionStatus.REQUESTED)
                    .build();

            SessionResponse response = SessionMapper.toResponse(session);

            assertNull(response.cancelReason());
        }
    }

    @Nested
    @DisplayName("ReviewMapper")
    class ReviewMapperTests {

        @Test
        @DisplayName("should map Review entity to ReviewResponse")
        void mapReview() {
            Review review = Review.builder()
                    .id(1L).sessionId(10L).mentorId(20L)
                    .reviewerId(30L).rating(5)
                    .comment("Excellent session!")
                    .build();

            ReviewResponse response = ReviewMapper.toResponse(review);

            assertEquals(1L, response.id());
            assertEquals(10L, response.sessionId());
            assertEquals(20L, response.mentorId());
            assertEquals(30L, response.reviewerId());
            assertEquals(5, response.rating());
            assertEquals("Excellent session!", response.comment());
        }
    }
}
