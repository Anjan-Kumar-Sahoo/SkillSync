package com.skillsync.session.service;

import com.skillsync.session.config.RabbitMQConfig;
import com.skillsync.session.dto.*;
import com.skillsync.session.entity.Session;
import com.skillsync.session.enums.SessionStatus;
import com.skillsync.session.event.SessionEvent;
import com.skillsync.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service @RequiredArgsConstructor @Slf4j
public class SessionService {
    private final SessionRepository sessionRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public SessionResponse createSession(Long learnerId, CreateSessionRequest request) {
        if (learnerId.equals(request.mentorId())) {
            throw new RuntimeException("Cannot book a session with yourself");
        }
        if (request.sessionDate().isBefore(LocalDateTime.now().plusHours(24))) {
            throw new RuntimeException("Session must be booked at least 24 hours in advance");
        }

        LocalDateTime endTime = request.sessionDate().plusMinutes(request.durationMinutes());
        List<Session> conflicts = sessionRepository.findConflictingSessions(
                request.mentorId(), request.sessionDate(), endTime);
        if (!conflicts.isEmpty()) {
            throw new RuntimeException("Mentor already has a session during this time slot");
        }

        Session session = Session.builder()
                .mentorId(request.mentorId()).learnerId(learnerId)
                .topic(request.topic()).description(request.description())
                .sessionDate(request.sessionDate()).durationMinutes(request.durationMinutes())
                .status(SessionStatus.REQUESTED).build();
        session = sessionRepository.save(session);

        publishEvent(session, "session.requested");
        log.info("Session {} created by learner {}", session.getId(), learnerId);
        return mapToResponse(session);
    }

    @Transactional
    public SessionResponse acceptSession(Long sessionId, Long mentorId) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.ACCEPTED);
        session.setStatus(SessionStatus.ACCEPTED);
        session = sessionRepository.save(session);
        publishEvent(session, "session.accepted");
        return mapToResponse(session);
    }

    @Transactional
    public SessionResponse rejectSession(Long sessionId, Long mentorId, String reason) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.REJECTED);
        session.setStatus(SessionStatus.REJECTED);
        session.setCancelReason(reason);
        session = sessionRepository.save(session);
        publishEvent(session, "session.rejected");
        return mapToResponse(session);
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
        publishEvent(session, "session.cancelled");
        return mapToResponse(session);
    }

    @Transactional
    public SessionResponse completeSession(Long sessionId, Long mentorId) {
        Session session = getAndValidateOwnership(sessionId, mentorId, true);
        validateTransition(session, SessionStatus.COMPLETED);
        session.setStatus(SessionStatus.COMPLETED);
        session = sessionRepository.save(session);
        publishEvent(session, "session.completed");
        return mapToResponse(session);
    }

    public SessionResponse getSessionById(Long id) {
        return mapToResponse(sessionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Session not found: " + id)));
    }

    public Page<SessionResponse> getSessionsByLearner(Long learnerId, Pageable pageable) {
        return sessionRepository.findByLearnerId(learnerId, pageable).map(this::mapToResponse);
    }

    public Page<SessionResponse> getSessionsByMentor(Long mentorId, Pageable pageable) {
        return sessionRepository.findByMentorId(mentorId, pageable).map(this::mapToResponse);
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
                    session.getLearnerId(), session.getTopic(), session.getStatus().name(), session.getCancelReason());
            rabbitTemplate.convertAndSend(RabbitMQConfig.SESSION_EXCHANGE, routingKey, event);
        } catch (Exception e) {
            log.error("Failed to publish session event: {}", e.getMessage());
        }
    }

    private SessionResponse mapToResponse(Session s) {
        return new SessionResponse(s.getId(), s.getMentorId(), s.getLearnerId(), s.getTopic(),
                s.getDescription(), s.getSessionDate(), s.getDurationMinutes(), s.getMeetingLink(),
                s.getStatus().name(), s.getCancelReason(), s.getCreatedAt());
    }
}
