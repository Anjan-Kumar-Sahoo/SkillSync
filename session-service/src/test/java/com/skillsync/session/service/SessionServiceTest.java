package com.skillsync.session.service;

import com.skillsync.session.dto.CreateSessionRequest;
import com.skillsync.session.dto.SessionResponse;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private SessionService sessionService;

    private Session testSession;

    @BeforeEach
    void setUp() {
        testSession = Session.builder()
                .id(1L)
                .mentorId(2L)
                .learnerId(3L)
                .topic("Java Basics")
                .description("Learn Java")
                .sessionDate(LocalDateTime.now().plusDays(2))
                .durationMinutes(60)
                .status(SessionStatus.REQUESTED)
                .build();
    }

    @Test
    @DisplayName("Create session - success")
    void createSession_shouldCreateAndReturn() {
        CreateSessionRequest request = new CreateSessionRequest(2L, "Java Basics", "Learn Java",
                LocalDateTime.now().plusDays(2), 60);

        when(sessionRepository.findConflictingSessions(anyLong(), any(), any())).thenReturn(Collections.emptyList());
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        SessionResponse response = sessionService.createSession(3L, request);

        assertNotNull(response);
        assertEquals("Java Basics", response.topic());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Create session - self booking throws exception")
    void createSession_shouldThrowForSelfBooking() {
        CreateSessionRequest request = new CreateSessionRequest(3L, "Topic", "Desc",
                LocalDateTime.now().plusDays(2), 60);

        assertThrows(RuntimeException.class, () -> sessionService.createSession(3L, request));
    }

    @Test
    @DisplayName("Get session by ID - success")
    void getSessionById_shouldReturnSession() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));

        SessionResponse response = sessionService.getSessionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Get session by ID - not found throws exception")
    void getSessionById_shouldThrowWhenNotFound() {
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> sessionService.getSessionById(999L));
    }

    @Test
    @DisplayName("Cancel session - unauthorized throws exception")
    void cancelSession_shouldThrowWhenUnauthorized() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));

        assertThrows(RuntimeException.class, () -> sessionService.cancelSession(1L, 999L));
    }
}
