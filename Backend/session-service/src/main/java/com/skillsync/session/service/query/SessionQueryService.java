package com.skillsync.session.service.query;

import com.skillsync.cache.CacheService;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.mapper.SessionMapper;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

/**
 * CQRS Query Service for Session operations.
 * Cache-aside with stampede + penetration protection (5-minute TTL).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionQueryService {

    private final SessionRepository sessionRepository;
    private final CacheService cacheService;

    @Value("${cache.ttl.session:300}")
    private long sessionTtl;

    /**
     * Cache-aside with stampede protection: get session by ID.
     */
    public SessionResponse getSessionById(Long id) {
        String cacheKey = CacheService.vKey("session:" + id);

        return cacheService.getOrLoad(cacheKey, SessionResponse.class,
                Duration.ofSeconds(sessionTtl), () -> {
                    Session session = sessionRepository.findById(id).orElse(null);
                    if (session == null) return null;
                    return mapToResponse(session);
                });
    }

    public Page<SessionResponse> getSessionsByLearner(Long learnerId, Pageable pageable) {
        return getSessionsByLearner(learnerId, null, pageable);
    }

    public Page<SessionResponse> getSessionsByLearner(Long learnerId, List<SessionStatus> statuses, Pageable pageable) {
        if (statuses != null && !statuses.isEmpty()) {
            return sessionRepository.findByLearnerIdAndStatusIn(learnerId, statuses, pageable)
                    .map(SessionQueryService::mapToResponse);
        }

        return sessionRepository.findByLearnerId(learnerId, pageable)
                .map(SessionQueryService::mapToResponse);
    }

    public long getSessionCount() {
        return sessionRepository.count();
    }

    public Page<SessionResponse> getSessionsByMentor(Long mentorId, Pageable pageable) {
        return getSessionsByMentor(mentorId, null, pageable);
    }

    public Page<SessionResponse> getSessionsByMentor(Long mentorId, List<SessionStatus> statuses, Pageable pageable) {
        if (statuses != null && !statuses.isEmpty()) {
            return sessionRepository.findByMentorIdAndStatusIn(mentorId, statuses, pageable)
                    .map(SessionQueryService::mapToResponse);
        }

        return sessionRepository.findByMentorId(mentorId, pageable)
                .map(SessionQueryService::mapToResponse);
    }

    public java.util.List<SessionResponse> getActiveSessionsForMentor(Long mentorId) {
        return sessionRepository.findByMentorIdAndStatusIn(mentorId, 
            java.util.List.of(com.skillsync.session.enums.SessionStatus.REQUESTED, 
                              com.skillsync.session.enums.SessionStatus.ACCEPTED))
                .stream().map(SessionQueryService::mapToResponse)
                .toList();
    }

    /**
     * @deprecated Use {@link SessionMapper#toResponse} directly.
     */
    @Deprecated
    public static SessionResponse mapToResponse(Session s) {
        return SessionMapper.toResponse(s);
    }
}
