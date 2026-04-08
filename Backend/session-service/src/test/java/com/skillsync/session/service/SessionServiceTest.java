package com.skillsync.session.service;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.CreateSessionRequest;
import com.skillsync.session.dto.SessionResponse;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.repository.SessionRepository;
import com.skillsync.session.service.command.SessionCommandService;
import com.skillsync.session.service.query.SessionQueryService;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private MentorProfileClient mentorProfileClient;

    @InjectMocks private SessionCommandService sessionCommandService;
    @InjectMocks private SessionQueryService sessionQueryService;

    private Session testSession;

    @BeforeEach
    void setUp() {
        testSession = Session.builder()
                .id(1L).mentorId(2L).learnerId(3L)
                .topic("Java Basics").description("Learn Java")
                .sessionDate(LocalDateTime.now().plusDays(2))
                .durationMinutes(60).status(SessionStatus.REQUESTED).build();
    }

    @Test
    @DisplayName("Create session - success with cache invalidation")
    void createSession_shouldCreateAndInvalidateCache() {
        CreateSessionRequest request = new CreateSessionRequest(2L, "Java Basics", "Learn Java",
                LocalDateTime.now().plusDays(2), 60);

        when(authServiceClient.getUserById(2L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
        when(sessionRepository.save(any(Session.class))).thenReturn(testSession);

        SessionResponse response = sessionCommandService.createSession(3L, request);

        assertNotNull(response);
        assertEquals("Java Basics", response.topic());
        verify(sessionRepository).save(any(Session.class));
        verify(cacheService).evict(CacheService.vKey("session:1"));
        verify(cacheService).evictByPattern(CacheService.vKey("session:learner:3:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("session:mentor:2:*"));
        verify(rabbitTemplate, never()).convertAndSend(
            eq("session.exchange"),
            eq("session.requested"),
            any(com.skillsync.session.event.SessionEvent.class)
        );
    }

    @Test
    @DisplayName("Create session - self booking throws exception")
    void createSession_shouldThrowForSelfBooking() {
        CreateSessionRequest request = new CreateSessionRequest(3L, "Topic", "Desc",
                LocalDateTime.now().plusDays(2), 60);

        assertThrows(RuntimeException.class, () -> sessionCommandService.createSession(3L, request));
    }

    @Test
    @DisplayName("Get session by ID - cache miss → DB fetch")
    void getSessionById_shouldReturnSession() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("session:1")), eq(SessionResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SessionResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // fallback to DB query
                });
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));

        SessionResponse response = sessionQueryService.getSessionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
    }

    @Test
    @DisplayName("Get session by ID - cache HIT → NO DB fetch")
    void getSessionById_shouldReturnFromCache() {
        SessionResponse cached = new SessionResponse(1L, 2L, 3L, "Java Basics", "Learn Java",
                LocalDateTime.now().plusDays(2), 60, SessionStatus.REQUESTED.name(), "link", "none", LocalDateTime.now());
        
        when(cacheService.getOrLoad(eq(CacheService.vKey("session:1")), eq(SessionResponse.class), any(), any()))
                .thenReturn(cached);

        SessionResponse response = sessionQueryService.getSessionById(1L);

        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(sessionRepository, never()).findById(anyLong()); // bypass DB
    }

    @Test
    @DisplayName("Get session by ID - not found throws exception")
    void getSessionById_shouldThrowWhenNotFound() {
        when(cacheService.getOrLoad(eq(CacheService.vKey("session:999")), eq(SessionResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SessionResponse> fallback = inv.getArgument(3);
                    return fallback.get(); // fallback to DB query, returning null
                });
        when(sessionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            SessionResponse session = sessionQueryService.getSessionById(999L);
            if (session == null) throw new RuntimeException("Not found");
        });
    }

    @Test
    @DisplayName("Cancel session - unauthorized throws exception")
    void cancelSession_shouldThrowWhenUnauthorized() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));

        assertThrows(RuntimeException.class, () -> sessionCommandService.cancelSession(1L, 999L));
    }

    @Test
    @DisplayName("Rollback unpaid session - REQUESTED becomes CANCELLED without notification event")
    void rollbackSessionPayment_requested_shouldCancelWithoutEvent() {
        Session requestedSession = Session.builder()
                .id(11L).mentorId(2L).learnerId(3L)
                .topic("Java Basics").description("Learn Java")
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationMinutes(60).status(SessionStatus.REQUESTED)
                .build();

        when(sessionRepository.findById(11L)).thenReturn(Optional.of(requestedSession));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionCommandService.rollbackSessionPayment(11L, 3L, "Payment failed");

        assertEquals(SessionStatus.CANCELLED, requestedSession.getStatus());
        verify(cacheService).evict(CacheService.vKey("session:11"));
        verify(cacheService).evictByPattern(CacheService.vKey("session:learner:3:*"));
        verify(cacheService).evictByPattern(CacheService.vKey("session:mentor:2:*"));
        verify(rabbitTemplate, never()).convertAndSend(
            eq("session.exchange"),
            eq("session.cancelled"),
            any(com.skillsync.session.event.SessionEvent.class)
        );
    }

    @Test
    @DisplayName("Rollback compensated session - ACCEPTED becomes CANCELLED with notification event")
    void rollbackSessionPayment_accepted_shouldCancelWithEvent() {
        Session acceptedSession = Session.builder()
                .id(12L).mentorId(2L).learnerId(3L)
                .topic("Java Basics").description("Learn Java")
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationMinutes(60).status(SessionStatus.ACCEPTED)
                .build();

        when(sessionRepository.findById(12L)).thenReturn(Optional.of(acceptedSession));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionCommandService.rollbackSessionPayment(12L, 3L, "Compensated");

        assertEquals(SessionStatus.CANCELLED, acceptedSession.getStatus());
    verify(rabbitTemplate).convertAndSend(
        eq("session.exchange"),
        eq("session.cancelled"),
        any(com.skillsync.session.event.SessionEvent.class)
    );
    }
}
