package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.repository.SessionRepository;
import com.skillsync.session.service.MentorMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionCommandServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private CacheService cacheService;
    @Mock private AuthServiceClient authServiceClient;
    @Mock private MentorProfileClient mentorProfileClient;
    @Mock private MentorMetricsService mentorMetricsService;
    @InjectMocks private SessionCommandService service;

    private Session testSession;

    @BeforeEach
    void setUp() {
        testSession = Session.builder()
                .id(1L).mentorId(10L).learnerId(20L)
                .topic("Java").description("Learn Java")
                .sessionDate(LocalDateTime.now().plusDays(1))
                .durationMinutes(60).status(SessionStatus.REQUESTED)
                .defaultRatingApplied(false).build();
    }

    @Nested @DisplayName("createSession")
    class CreateSessionTests {
        @Test @DisplayName("Success - direct mentor")
        void success() {
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(sessionRepository.existsByMentorIdAndLearnerIdAndSessionDateAndStatusIn(eq(10L), eq(20L), any(), any())).thenReturn(false);
            when(sessionRepository.save(any(Session.class))).thenReturn(testSession);
            var req = new CreateSessionRequest(10L, "Java", "Learn", LocalDateTime.now().plusDays(1), 60);
            assertNotNull(service.createSession(20L, req));
            verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.SESSION_EXCHANGE), eq("session.requested"), (Object) any());
        }

        @Test @DisplayName("Self-booking throws")
        void selfBooking() {
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            var req = new CreateSessionRequest(10L, "Java", "Learn", LocalDateTime.now(), 60);
            assertThrows(RuntimeException.class, () -> service.createSession(10L, req));
        }

        @Test @DisplayName("Duplicate slot throws")
        void duplicateSlot() {
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(sessionRepository.existsByMentorIdAndLearnerIdAndSessionDateAndStatusIn(eq(10L), eq(20L), any(), any())).thenReturn(true);
            var req = new CreateSessionRequest(10L, "Java", "Learn", LocalDateTime.now().plusDays(1), 60);
            assertThrows(RuntimeException.class, () -> service.createSession(20L, req));
        }

        @Test @DisplayName("Null mentor ID throws")
        void nullMentorId() {
            var req = new CreateSessionRequest(null, "Java", "Learn", LocalDateTime.now(), 60);
            assertThrows(RuntimeException.class, () -> service.createSession(20L, req));
        }

        @Test @DisplayName("Mentor resolved via profile lookup")
        void resolvedViaProfile() {
            when(authServiceClient.getUserById(50L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            when(mentorProfileClient.getMentorById(50L)).thenReturn(Map.of("userId", 10L));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_MENTOR"));
            when(sessionRepository.existsByMentorIdAndLearnerIdAndSessionDateAndStatusIn(eq(10L), eq(20L), any(), any())).thenReturn(false);
            when(sessionRepository.save(any(Session.class))).thenReturn(testSession);
            var req = new CreateSessionRequest(50L, "Java", "Learn", LocalDateTime.now().plusDays(1), 60);
            assertNotNull(service.createSession(20L, req));
        }

        @Test @DisplayName("Mentor resolution fails")
        void resolutionFails() {
            when(authServiceClient.getUserById(50L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            when(mentorProfileClient.getMentorById(50L)).thenThrow(new RuntimeException("Not found"));
            var req = new CreateSessionRequest(50L, "Java", "Learn", LocalDateTime.now(), 60);
            assertThrows(RuntimeException.class, () -> service.createSession(20L, req));
        }

        @Test @DisplayName("Auth unavailable - fallback to mapped userId")
        void authUnavailable() {
            when(authServiceClient.getUserById(50L)).thenThrow(new RuntimeException("down"));
            when(mentorProfileClient.getMentorById(50L)).thenReturn(Map.of("userId", "10"));
            when(authServiceClient.getUserById(10L)).thenThrow(new RuntimeException("down"));
            when(sessionRepository.existsByMentorIdAndLearnerIdAndSessionDateAndStatusIn(eq(10L), eq(20L), any(), any())).thenReturn(false);
            when(sessionRepository.save(any(Session.class))).thenReturn(testSession);
            var req = new CreateSessionRequest(50L, "Java", "Learn", LocalDateTime.now().plusDays(1), 60);
            assertNotNull(service.createSession(20L, req));
        }

        @Test @DisplayName("Mapped user not a mentor throws")
        void mappedNotMentor() {
            when(authServiceClient.getUserById(50L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            when(mentorProfileClient.getMentorById(50L)).thenReturn(Map.of("userId", 10L));
            when(authServiceClient.getUserById(10L)).thenReturn(Map.of("role", "ROLE_LEARNER"));
            var req = new CreateSessionRequest(50L, "Java", "Learn", LocalDateTime.now(), 60);
            assertThrows(RuntimeException.class, () -> service.createSession(20L, req));
        }
    }

    @Nested @DisplayName("acceptSession")
    class AcceptTests {
        @Test @DisplayName("Success")
        void success() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            assertNotNull(service.acceptSession(1L, 10L));
            assertEquals(SessionStatus.ACCEPTED, testSession.getStatus());
        }
        @Test @DisplayName("Not found")
        void notFound() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.acceptSession(1L, 10L));
        }
        @Test @DisplayName("Wrong mentor")
        void wrongMentor() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            assertThrows(RuntimeException.class, () -> service.acceptSession(1L, 999L));
        }
    }

    @Nested @DisplayName("rejectSession")
    class RejectTests {
        @Test @DisplayName("Success")
        void success() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            assertNotNull(service.rejectSession(1L, 10L, "Busy"));
            assertEquals(SessionStatus.REJECTED, testSession.getStatus());
        }
    }

    @Nested @DisplayName("cancelSession")
    class CancelTests {
        @Test @DisplayName("By mentor")
        void byMentor() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            assertNotNull(service.cancelSession(1L, 10L));
        }
        @Test @DisplayName("Unauthorized")
        void unauthorized() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            assertThrows(RuntimeException.class, () -> service.cancelSession(1L, 999L));
        }
    }

    @Nested @DisplayName("completeSession")
    class CompleteTests {
        @Test @DisplayName("Success")
        void success() {
            testSession.setStatus(SessionStatus.ACCEPTED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            when(mentorMetricsService.calculateMentorMetrics(10L))
                    .thenReturn(new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false));
            assertNotNull(service.completeSession(1L, 10L));
            assertTrue(testSession.isDefaultRatingApplied());
        }
        @Test @DisplayName("Metrics failure swallowed")
        void metricsFailure() {
            testSession.setStatus(SessionStatus.ACCEPTED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            when(mentorMetricsService.calculateMentorMetrics(10L)).thenThrow(new RuntimeException("err"));
            assertDoesNotThrow(() -> service.completeSession(1L, 10L));
        }
    }

    @Nested @DisplayName("confirmSessionPayment")
    class ConfirmPaymentTests {
        @Test @DisplayName("Success")
        void success() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            service.confirmSessionPayment(1L);
            assertEquals(SessionStatus.ACCEPTED, testSession.getStatus());
            assertNotNull(testSession.getMeetingLink());
        }
        @Test @DisplayName("Not found")
        void notFound() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
            assertThrows(RuntimeException.class, () -> service.confirmSessionPayment(1L));
        }
    }

    @Nested @DisplayName("rollbackSessionPayment")
    class RollbackTests {
        @Test @DisplayName("REQUESTED -> CANCELLED, no event published")
        void requested() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            service.rollbackSessionPayment(1L, 20L, "Failed");
            assertEquals(SessionStatus.CANCELLED, testSession.getStatus());
            verify(rabbitTemplate, never()).convertAndSend(anyString(), eq("session.cancelled"), (Object) any());
        }
        @Test @DisplayName("ACCEPTED -> CANCELLED, event published")
        void accepted() {
            testSession.setStatus(SessionStatus.ACCEPTED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            service.rollbackSessionPayment(1L, 20L, "Refund");
            verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.SESSION_EXCHANGE), eq("session.cancelled"), (Object) any());
        }
        @Test @DisplayName("Null userId skips auth check")
        void nullUserId() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            assertDoesNotThrow(() -> service.rollbackSessionPayment(1L, null, "System"));
        }
        @Test @DisplayName("Unauthorized throws")
        void unauthorized() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            assertThrows(RuntimeException.class, () -> service.rollbackSessionPayment(1L, 999L, "x"));
        }
        @Test @DisplayName("Skips CANCELLED")
        void skipsCancelled() {
            testSession.setStatus(SessionStatus.CANCELLED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            service.rollbackSessionPayment(1L, 20L, "x");
            verify(sessionRepository, never()).save(any());
        }
        @Test @DisplayName("Skips COMPLETED")
        void skipsCompleted() {
            testSession.setStatus(SessionStatus.COMPLETED);
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            service.rollbackSessionPayment(1L, 20L, "x");
            verify(sessionRepository, never()).save(any());
        }
        @Test @DisplayName("Blank reason uses default")
        void blankReason() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            service.rollbackSessionPayment(1L, 20L, "  ");
            assertTrue(testSession.getCancelReason().contains("Rolled back"));
        }
        @Test @DisplayName("Null reason uses default")
        void nullReason() {
            when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
            when(sessionRepository.save(any())).thenReturn(testSession);
            service.rollbackSessionPayment(1L, 20L, null);
            assertTrue(testSession.getCancelReason().contains("Rolled back"));
        }
    }

    @Test @DisplayName("publishEvent swallows exceptions")
    void publishEventSwallows() {
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any())).thenReturn(testSession);
        doThrow(new RuntimeException("down")).when(rabbitTemplate).convertAndSend(anyString(), anyString(), (Object) any());
        assertDoesNotThrow(() -> service.cancelSession(1L, 20L));
    }

    @Test @DisplayName("Null session date handled in publishEvent")
    void nullSessionDate() {
        testSession.setSessionDate(null);
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        when(sessionRepository.save(any())).thenReturn(testSession);
        assertDoesNotThrow(() -> service.cancelSession(1L, 20L));
    }
}
