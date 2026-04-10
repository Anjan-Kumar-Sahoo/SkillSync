package com.skillsync.session.service.command;

import com.skillsync.cache.CacheService;
import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.event.SessionEvent;
import com.skillsync.session.feign.AuthServiceClient;
import com.skillsync.session.feign.MentorProfileClient;
import com.skillsync.session.mapper.SessionMapper;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * CQRS Command Service for Session operations.
 * Handles all write operations, cache invalidation, and event publishing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionCommandService {

    private final SessionRepository sessionRepository;
    private final RabbitTemplate rabbitTemplate;
    private final CacheService cacheService;
    private final AuthServiceClient authServiceClient;
    private final MentorProfileClient mentorProfileClient;

    @Transactional
    public SessionResponse createSession(Long learnerId, CreateSessionRequest request) {
        Long mentorUserId = resolveMentorUserId(request.mentorId());

        if (learnerId.equals(mentorUserId)) {
            throw new RuntimeException("Cannot book a session with yourself");
        }


        Session session = Session.builder()
                .mentorId(mentorUserId).learnerId(learnerId)
                .topic(request.topic()).description(request.description())
                .sessionDate(request.sessionDate()).durationMinutes(request.durationMinutes())
                .status(SessionStatus.REQUESTED).build();
        session = sessionRepository.save(session);

        // Invalidate user session caches
        invalidateSessionCaches(session);

        // Notify mentor and learner that the request has been created.
        publishEvent(session, "session.requested");

        log.info("[CQRS:COMMAND] Session {} created in REQUESTED state (awaiting payment confirmation). Cache invalidated.",
            session.getId());
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse acceptSession(Long sessionId, Long mentorId) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.ACCEPTED);
        session.setStatus(SessionStatus.ACCEPTED);
        session = sessionRepository.save(session);

        invalidateSessionCaches(session);
        publishEvent(session, "session.accepted");
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public void confirmSessionPayment(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        // Transition to ACCEPTED automatically on payment success
        validateTransition(session, SessionStatus.ACCEPTED);
        session.setStatus(SessionStatus.ACCEPTED);
        // Optionally generate a meeting link here
        session.setMeetingLink("https://meet.jit.si/SkillSync-" + session.getId() + "-" + System.currentTimeMillis());
        session = sessionRepository.save(session);

        invalidateSessionCaches(session);
        // Publish event to notify learner and mentor (notification service listens to session.accepted)
        publishEvent(session, "session.accepted");
    }

    @Transactional
    public SessionResponse rejectSession(Long sessionId, Long mentorId, String reason) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.REJECTED);
        session.setStatus(SessionStatus.REJECTED);
        session.setCancelReason(reason);
        session = sessionRepository.save(session);

        invalidateSessionCaches(session);
        publishEvent(session, "session.rejected");
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse cancelSession(Long sessionId, Long userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        if (!session.getMentorId().equals(userId) && !session.getLearnerId().equals(userId)) {
            throw new RuntimeException("Not authorized to cancel this session");
        }
        validateTransition(session, SessionStatus.CANCELLED);
        session.setStatus(SessionStatus.CANCELLED);
        session = sessionRepository.save(session);

        invalidateSessionCaches(session);
        publishEvent(session, "session.cancelled");
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public SessionResponse completeSession(Long sessionId, Long mentorId) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.COMPLETED);
        session.setStatus(SessionStatus.COMPLETED);
        session = sessionRepository.save(session);

        invalidateSessionCaches(session);
        publishEvent(session, "session.completed");
        return SessionMapper.toResponse(session);
    }

    @Transactional
    public void rollbackSessionPayment(Long sessionId, Long userId, String reason) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));

        if (userId != null && !userId.equals(session.getLearnerId())) {
            throw new RuntimeException("Not authorized to rollback this session payment");
        }

        SessionStatus currentStatus = session.getStatus();
        if (currentStatus == SessionStatus.CANCELLED || currentStatus == SessionStatus.REJECTED || currentStatus == SessionStatus.COMPLETED) {
            log.info("Skipping payment rollback for session {} because status is {}", sessionId, currentStatus);
            return;
        }

        if (!session.isTransitionAllowed(SessionStatus.CANCELLED)) {
            log.warn("Skipping payment rollback for session {} due to invalid transition {} -> CANCELLED",
                    sessionId, currentStatus);
            return;
        }

        session.setStatus(SessionStatus.CANCELLED);
        session.setCancelReason(reason != null && !reason.isBlank()
                ? reason
                : "Rolled back due to payment failure/cancellation");
        session = sessionRepository.save(session);
        invalidateSessionCaches(session);

        // Only emit cancellation event if session was already confirmed to avoid noisy mentor alerts
        // for unpaid REQUESTED sessions.
        if (currentStatus == SessionStatus.ACCEPTED) {
            publishEvent(session, "session.cancelled");
        }

        log.info("Rolled back session {} after payment issue. Previous status: {}", sessionId, currentStatus);
    }

    private void invalidateSessionCaches(Session session) {
        cacheService.evict(CacheService.vKey("session:" + session.getId()));
        cacheService.evictByPattern(CacheService.vKey("session:learner:" + session.getLearnerId() + ":*"));
        cacheService.evictByPattern(CacheService.vKey("session:mentor:" + session.getMentorId() + ":*"));
    }

    private Session getAndValidateOwnership(Long sessionId, Long userId, boolean isMentor) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found: " + sessionId));
        Long ownerId = isMentor ? session.getMentorId() : session.getLearnerId();
        if (!ownerId.equals(userId)) {
            throw new RuntimeException("Not authorized for this session");
        }
        return session;
    }

    private void validateTransition(Session session, SessionStatus target) {
        if (!session.isTransitionAllowed(target)) {
            throw new RuntimeException("Cannot transition from " + session.getStatus() + " to " + target);
        }
    }

    private void publishEvent(Session session, String routingKey) {
        try {
            SessionEvent event = new SessionEvent(session.getId(), session.getMentorId(),
                    session.getLearnerId(), session.getTopic(), session.getStatus().name(),
                    session.getCancelReason(),
                    session.getSessionDate() != null ? session.getSessionDate().toString() : null);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SESSION_EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish session event: {}", e.getMessage());
        }
    }

    private Long resolveMentorUserId(Long requestedMentorId) {
        if (requestedMentorId == null) {
            throw new RuntimeException("Mentor ID is required");
        }

        Map<String, Object> user = fetchUserById(requestedMentorId);
        if (isMentorUser(user)) {
            return requestedMentorId;
        }

        Long mappedUserId = mapMentorProfileIdToUserId(requestedMentorId);
        if (mappedUserId != null) {
            Map<String, Object> mappedUser = fetchUserById(mappedUserId);
            // If auth lookup is temporarily unavailable, fall back to mapped userId from mentor profile.
            if (mappedUser == null || isMentorUser(mappedUser)) {
                log.warn("Resolved mentor profile id {} to mentor user id {} for booking request.", requestedMentorId, mappedUserId);
                return mappedUserId;
            }
        }

        throw new RuntimeException("Invalid mentor id for booking: " + requestedMentorId);
    }

    private Map<String, Object> fetchUserById(Long userId) {
        try {
            return authServiceClient.getUserById(userId);
        } catch (Exception e) {
            log.warn("Unable to validate user {} via auth-service: {}", userId, e.getMessage());
            return null;
        }
    }

    private boolean isMentorUser(Map<String, Object> userPayload) {
        if (userPayload == null) {
            return false;
        }
        Object role = userPayload.get("role");
        return "ROLE_MENTOR".equals(String.valueOf(role));
    }

    private Long mapMentorProfileIdToUserId(Long mentorProfileId) {
        try {
            Map<String, Object> mentorProfile = mentorProfileClient.getMentorById(mentorProfileId);
            Object userIdValue = mentorProfile.get("userId");
            if (userIdValue instanceof Number number) {
                return number.longValue();
            }
            if (userIdValue != null) {
                return Long.parseLong(String.valueOf(userIdValue));
            }
        } catch (Exception e) {
            log.debug("Mentor profile lookup failed for id {}: {}", mentorProfileId, e.getMessage());
        }
        return null;
    }
}
