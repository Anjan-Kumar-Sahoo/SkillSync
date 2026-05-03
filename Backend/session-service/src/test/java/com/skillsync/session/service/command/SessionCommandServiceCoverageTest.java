package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.CreateSessionRequest;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.repository.SessionRepository;
import com.skillsync.session.service.MentorMetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionCommandServiceCoverageTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private MentorProfileClient mentorProfileClient;
    @Mock private MentorMetricsService mentorMetricsService;

    @InjectMocks private SessionCommandService sessionCommandService;

    @Test @DisplayName("createSession - null mentor id")
    void createSessionNullMentor() {
        CreateSessionRequest request = new CreateSessionRequest(null, "Topic", "Desc", LocalDateTime.now(), 60);
        assertThrows(RuntimeException.class, () -> sessionCommandService.createSession(1L, request));
    }

    @Test @DisplayName("createSession - mentor lookup failures")
    void resolveMentorUserIdFailures() {
        CreateSessionRequest request = new CreateSessionRequest(10L, "Topic", "Desc", LocalDateTime.now(), 60);
        when(authServiceClient.getUserById(10L)).thenThrow(new RuntimeException("Auth down"));
        when(mentorProfileClient.getMentorById(10L)).thenThrow(new RuntimeException("Mentor profile down"));
        
        assertThrows(RuntimeException.class, () -> sessionCommandService.createSession(1L, request));
    }

    @Test @DisplayName("rollbackSessionPayment - Skip if COMPLETED")
    void rollbackSkipIfCompleted() {
        Session session = Session.builder().id(100L).learnerId(1L).status(SessionStatus.COMPLETED).build();
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        sessionCommandService.rollbackSessionPayment(100L, 1L, "Reason");
        verify(sessionRepository, never()).save(any());
    }

    @Test @DisplayName("rollbackSessionPayment - Wrong user")
    void rollbackWrongUser() {
        Session session = Session.builder().id(100L).learnerId(1L).status(SessionStatus.REQUESTED).build();
        when(sessionRepository.findById(100L)).thenReturn(Optional.of(session));
        assertThrows(RuntimeException.class, () -> sessionCommandService.rollbackSessionPayment(100L, 99L, "Reason"));
    }

    @Test @DisplayName("rollbackSessionPayment - Invalid transition")
    void rollbackInvalidTransition() {
        Session mockSession = mock(Session.class);
        when(mockSession.getStatus()).thenReturn(SessionStatus.REQUESTED);
        when(mockSession.getLearnerId()).thenReturn(1L);
        when(mockSession.isTransitionAllowed(SessionStatus.CANCELLED)).thenReturn(false);
        when(sessionRepository.findById(101L)).thenReturn(Optional.of(mockSession));

        sessionCommandService.rollbackSessionPayment(101L, 1L, "Reason");
        verify(mockSession, never()).setStatus(any());
    }

    @Test @DisplayName("publishEvent exception branch")
    void publishEventException() {
        Session session = Session.builder().id(1L).mentorId(2L).learnerId(3L).status(SessionStatus.REQUESTED).build();
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(session));
        doThrow(new RuntimeException("Rabbit down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
        
        sessionCommandService.confirmSessionPayment(1L); 
        verify(sessionRepository, atLeastOnce()).findById(1L);
    }
}
