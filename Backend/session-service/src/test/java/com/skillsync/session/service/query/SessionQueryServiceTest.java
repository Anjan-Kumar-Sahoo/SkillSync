package com.skillsync.session.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionQueryServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private MentorMetricsService mentorMetricsService;
    @Mock private CacheService cacheService;
    @InjectMocks private SessionQueryService service;

    private Session testSession;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        testSession = Session.builder()
                .id(1L).mentorId(10L).learnerId(20L).topic("Java")
                .status(SessionStatus.REQUESTED).sessionDate(LocalDateTime.now())
                .durationMinutes(60).build();
        pageable = PageRequest.of(0, 10);
    }

    @Test @DisplayName("getSessionById - cache miss loads from DB")
    void getSessionById_cacheMiss() {
        when(cacheService.getOrLoad(anyString(), eq(SessionResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SessionResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(sessionRepository.findById(1L)).thenReturn(Optional.of(testSession));
        SessionResponse resp = service.getSessionById(1L);
        assertNotNull(resp);
    }

    @Test @DisplayName("getSessionById - not found returns null")
    void getSessionById_notFound() {
        when(cacheService.getOrLoad(anyString(), eq(SessionResponse.class), any(), any()))
                .thenAnswer(inv -> {
                    java.util.function.Supplier<SessionResponse> loader = inv.getArgument(3);
                    return loader.get();
                });
        when(sessionRepository.findById(1L)).thenReturn(Optional.empty());
        SessionResponse resp = service.getSessionById(1L);
        assertNull(resp);
    }

    @Test @DisplayName("getSessionsByLearner without statuses")
    void getSessionsByLearner_noStatuses() {
        Page<Session> page = new PageImpl<>(List.of(testSession));
        when(sessionRepository.findByLearnerId(20L, pageable)).thenReturn(page);
        Page<SessionResponse> result = service.getSessionsByLearner(20L, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test @DisplayName("getSessionsByLearner with statuses")
    void getSessionsByLearner_withStatuses() {
        Page<Session> page = new PageImpl<>(List.of(testSession));
        List<SessionStatus> statuses = List.of(SessionStatus.REQUESTED);
        when(sessionRepository.findByLearnerIdAndStatusIn(20L, statuses, pageable)).thenReturn(page);
        Page<SessionResponse> result = service.getSessionsByLearner(20L, statuses, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test @DisplayName("getSessionsByLearner with empty statuses falls back to all")
    void getSessionsByLearner_emptyStatuses() {
        Page<Session> page = new PageImpl<>(List.of(testSession));
        when(sessionRepository.findByLearnerId(20L, pageable)).thenReturn(page);
        Page<SessionResponse> result = service.getSessionsByLearner(20L, List.of(), pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test @DisplayName("getSessionsByMentor without statuses")
    void getSessionsByMentor_noStatuses() {
        Page<Session> page = new PageImpl<>(List.of(testSession));
        when(sessionRepository.findByMentorId(10L, pageable)).thenReturn(page);
        Page<SessionResponse> result = service.getSessionsByMentor(10L, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test @DisplayName("getSessionsByMentor with statuses")
    void getSessionsByMentor_withStatuses() {
        Page<Session> page = new PageImpl<>(List.of(testSession));
        List<SessionStatus> statuses = List.of(SessionStatus.ACCEPTED);
        when(sessionRepository.findByMentorIdAndStatusIn(10L, statuses, pageable)).thenReturn(page);
        Page<SessionResponse> result = service.getSessionsByMentor(10L, statuses, pageable);
        assertEquals(1, result.getTotalElements());
    }

    @Test @DisplayName("getSessionCount")
    void getSessionCount() {
        when(sessionRepository.countByStatus(SessionStatus.COMPLETED)).thenReturn(42L);
        assertEquals(42L, service.getSessionCount());
    }

    @Test @DisplayName("getMentorMetrics delegates")
    void getMentorMetrics() {
        MentorMetricsResponse metrics = new MentorMetricsResponse(10L, 5, 4.5, 3, 0, false);
        when(mentorMetricsService.calculateMentorMetrics(10L)).thenReturn(metrics);
        assertEquals(metrics, service.getMentorMetrics(10L));
    }

    @Test @DisplayName("getActiveSessionsForMentor")
    void getActiveSessions() {
        when(sessionRepository.findByMentorIdAndStatusIn(eq(10L), anyList())).thenReturn(List.of(testSession));
        List<SessionResponse> result = service.getActiveSessionsForMentor(10L);
        assertEquals(1, result.size());
    }
}
