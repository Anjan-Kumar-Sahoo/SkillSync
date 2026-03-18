//package com.skillsync.sessionservice.service;
//
//import com.skillsync.sessionservice.config.MentorClient;
//import com.skillsync.sessionservice.dto.request.SessionBookRequest;
//import com.skillsync.sessionservice.dto.response.SessionResponse;
//import com.skillsync.sessionservice.entity.Session;
//import com.skillsync.sessionservice.entity.SessionStatus;
//import com.skillsync.sessionservice.exception.InvalidSessionStateException;
//import com.skillsync.sessionservice.exception.SessionNotFoundException;
//import com.skillsync.sessionservice.mapper.SessionMapper;
//import com.skillsync.sessionservice.repository.SessionRepository;
//import com.skillsync.sessionservice.service.impl.SessionServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.amqp.rabbit.core.RabbitTemplate;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class SessionServiceImplTest {
//
//    @Mock private SessionRepository sessionRepository;
//    @Mock private SessionMapper sessionMapper;
//    @Mock private RabbitTemplate rabbitTemplate;
//    @Mock private MentorClient mentorClient;
//
//    @InjectMocks
//    private SessionServiceImpl sessionService;
//
//    private Session sampleSession;
//    private SessionResponse sampleResponse;
//
//    @BeforeEach
//    void setUp() {
//        sampleSession = Session.builder()
//                .id(1L)
//                .mentorId(10L)
//                .learnerId(20L)
//                .sessionDate(LocalDateTime.now().plusDays(1))
//                .status(SessionStatus.REQUESTED)
//                .topic("Spring Boot Microservices")
//                .build();
//
//        sampleResponse = new SessionResponse(
//                1L, 10L, 20L,
//                sampleSession.getSessionDate(),
//                SessionStatus.REQUESTED,
//                "Spring Boot Microservices",
//                LocalDateTime.now()
//        );
//    }
//
//    // ── bookSession ───────────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("bookSession: should save session and publish RabbitMQ event")
//    void bookSession_success() {
//        SessionBookRequest request = new SessionBookRequest(
//                10L, 20L, LocalDateTime.now().plusDays(1), "Spring Boot Microservices"
//        );
//
//        when(mentorClient.mentorExists(10L)).thenReturn(true);
//        when(sessionMapper.toEntity(request)).thenReturn(sampleSession);
//        when(sessionRepository.save(sampleSession)).thenReturn(sampleSession);
//        when(sessionMapper.toResponse(sampleSession)).thenReturn(sampleResponse);
//
//        SessionResponse result = sessionService.bookSession(request);
//
//        assertThat(result).isNotNull();
//        assertThat(result.getStatus()).isEqualTo(SessionStatus.REQUESTED);
//        verify(sessionRepository).save(any(Session.class));
//        verify(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));
//    }
//
//    @Test
//    @DisplayName("bookSession: should throw when mentor does not exist")
//    void bookSession_mentorNotFound() {
//        SessionBookRequest request = new SessionBookRequest(
//                99L, 20L, LocalDateTime.now().plusDays(1), "Topic"
//        );
//        when(mentorClient.mentorExists(99L)).thenReturn(false);
//
//        assertThatThrownBy(() -> sessionService.bookSession(request))
//                .isInstanceOf(IllegalArgumentException.class)
//                .hasMessageContaining("99");
//
//        verify(sessionRepository, never()).save(any());
//    }
//
//    // ── acceptSession ─────────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("acceptSession: REQUESTED → ACCEPTED")
//    void acceptSession_success() {
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//        SessionResponse acceptedResponse = new SessionResponse(
//                1L, 10L, 20L, sampleSession.getSessionDate(),
//                SessionStatus.ACCEPTED, "Spring Boot Microservices", LocalDateTime.now()
//        );
//        when(sessionRepository.save(sampleSession)).thenReturn(sampleSession);
//        when(sessionMapper.toResponse(sampleSession)).thenReturn(acceptedResponse);
//
//        SessionResponse result = sessionService.acceptSession(1L);
//
//        assertThat(result.getStatus()).isEqualTo(SessionStatus.ACCEPTED);
//        assertThat(sampleSession.getStatus()).isEqualTo(SessionStatus.ACCEPTED);
//    }
//
//    @Test
//    @DisplayName("acceptSession: should throw when session is not REQUESTED")
//    void acceptSession_invalidState() {
//        sampleSession.setStatus(SessionStatus.CANCELLED);
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//
//        assertThatThrownBy(() -> sessionService.acceptSession(1L))
//                .isInstanceOf(InvalidSessionStateException.class)
//                .hasMessageContaining("CANCELLED");
//    }
//
//    @Test
//    @DisplayName("acceptSession: should throw when session not found")
//    void acceptSession_notFound() {
//        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());
//
//        assertThatThrownBy(() -> sessionService.acceptSession(99L))
//                .isInstanceOf(SessionNotFoundException.class)
//                .hasMessageContaining("99");
//    }
//
//    // ── rejectSession ─────────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("rejectSession: REQUESTED → REJECTED")
//    void rejectSession_success() {
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//        SessionResponse rejectedResponse = new SessionResponse(
//                1L, 10L, 20L, sampleSession.getSessionDate(),
//                SessionStatus.REJECTED, "Topic", LocalDateTime.now()
//        );
//        when(sessionRepository.save(sampleSession)).thenReturn(sampleSession);
//        when(sessionMapper.toResponse(sampleSession)).thenReturn(rejectedResponse);
//
//        SessionResponse result = sessionService.rejectSession(1L);
//
//        assertThat(result.getStatus()).isEqualTo(SessionStatus.REJECTED);
//        assertThat(sampleSession.getStatus()).isEqualTo(SessionStatus.REJECTED);
//    }
//
//    @Test
//    @DisplayName("rejectSession: should throw when session is already ACCEPTED")
//    void rejectSession_invalidState() {
//        sampleSession.setStatus(SessionStatus.ACCEPTED);
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//
//        assertThatThrownBy(() -> sessionService.rejectSession(1L))
//                .isInstanceOf(InvalidSessionStateException.class);
//    }
//
//    // ── cancelSession ─────────────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("cancelSession: ACCEPTED → CANCELLED")
//    void cancelSession_fromAccepted() {
//        sampleSession.setStatus(SessionStatus.ACCEPTED);
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//        SessionResponse cancelledResponse = new SessionResponse(
//                1L, 10L, 20L, sampleSession.getSessionDate(),
//                SessionStatus.CANCELLED, "Topic", LocalDateTime.now()
//        );
//        when(sessionRepository.save(sampleSession)).thenReturn(sampleSession);
//        when(sessionMapper.toResponse(sampleSession)).thenReturn(cancelledResponse);
//
//        SessionResponse result = sessionService.cancelSession(1L);
//
//        assertThat(result.getStatus()).isEqualTo(SessionStatus.CANCELLED);
//    }
//
//    @Test
//    @DisplayName("cancelSession: should throw when session is COMPLETED")
//    void cancelSession_alreadyCompleted() {
//        sampleSession.setStatus(SessionStatus.COMPLETED);
//        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
//
//        assertThatThrownBy(() -> sessionService.cancelSession(1L))
//                .isInstanceOf(InvalidSessionStateException.class)
//                .hasMessageContaining("COMPLETED");
//    }
//
//    // ── getSessionsByUserId ───────────────────────────────────────────────────
//
//    @Test
//    @DisplayName("getSessionsByUserId: returns sessions for a user as learner or mentor")
//    void getSessionsByUserId_success() {
//        when(sessionRepository.findByLearnerIdOrMentorId(20L, 20L))
//                .thenReturn(List.of(sampleSession));
//        when(sessionMapper.toResponse(sampleSession)).thenReturn(sampleResponse);
//
//        List<SessionResponse> results = sessionService.getSessionsByUserId(20L);
//
//        assertThat(results).hasSize(1);
//        assertThat(results.get(0).getLearnerId()).isEqualTo(20L);
//    }
//}
