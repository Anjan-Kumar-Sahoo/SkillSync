package com.skillsync.sessionservice.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.skillsync.sessionservice.repository.SessionRepository;
import com.skillsync.sessionservice.service.SessionService;

//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;

//@Slf4j
@Service
//@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

	@Autowired
    private SessionRepository sessionRepository;
//    private final SessionMapper sessionMapper;
//    private final RabbitTemplate rabbitTemplate;
//    private final MentorClient mentorClient;
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // POST /sessions  →  Book a new session (state: REQUESTED)
//    // ─────────────────────────────────────────────────────────────────────────
//    @Override
//    @Transactional
//    public SessionResponse bookSession(SessionBookRequest request) {
//        log.info("Booking session: mentorId={}, learnerId={}", request.getMentorId(), request.getLearnerId());
//
//        // Validate mentor exists via Feign (circuit breaker applied via fallback)
//        try {
//            Boolean mentorExists = mentorClient.mentorExists(request.getMentorId());
//
//            if (!Boolean.TRUE.equals(mentorExists)) {
//                throw new MentorNotFoundException(
//                    "Mentor with id " + request.getMentorId() + " does not exist."
//                );
//            }
//
//        } catch (MentorNotFoundException ex) {
//            throw ex; // rethrow (business exception)
//
//        } catch (Exception ex) {
//            log.error("Mentor validation failed due to service issue: {}", ex.getMessage());
//
//            throw new ServiceUnavailableException(
//                    "Mentor service is currently unavailable. Please try again later."
//            );
//        }
//
//        Session session = sessionMapper.toEntity(request);
//        Session savedSession = sessionRepository.save(session);
//
//        // Publish SESSION_BOOKED event to RabbitMQ
//        publishSessionBookedEvent(savedSession);
//
//        log.info("Session booked successfully with id={}", savedSession.getId());
//        return sessionMapper.toResponse(savedSession);
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // PUT /sessions/{id}/accept  →  REQUESTED → ACCEPTED
//    // ─────────────────────────────────────────────────────────────────────────
//    @Override
//    @Transactional
//    public SessionResponse acceptSession(Long sessionId) {
//        log.info("Accepting session id={}", sessionId);
//        Session session = findSessionOrThrow(sessionId);
//
//        if (session.getStatus() != SessionStatus.REQUESTED) {
//            throw new InvalidSessionStateException(
//                    "Cannot accept session in status: " + session.getStatus() +
//                    ". Only REQUESTED sessions can be accepted."
//            );
//        }
//
//        session.setStatus(SessionStatus.ACCEPTED);
//        return sessionMapper.toResponse(sessionRepository.save(session));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // PUT /sessions/{id}/reject  →  REQUESTED → REJECTED
//    // ─────────────────────────────────────────────────────────────────────────
//    @Override
//    @Transactional
//    public SessionResponse rejectSession(Long sessionId) {
//        log.info("Rejecting session id={}", sessionId);
//        Session session = findSessionOrThrow(sessionId);
//
//        if (session.getStatus() != SessionStatus.REQUESTED) {
//            throw new InvalidSessionStateException(
//                    "Cannot reject session in status: " + session.getStatus() +
//                    ". Only REQUESTED sessions can be rejected."
//            );
//        }
//
//        session.setStatus(SessionStatus.REJECTED);
//        return sessionMapper.toResponse(sessionRepository.save(session));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // PUT /sessions/{id}/cancel  →  REQUESTED or ACCEPTED → CANCELLED
//    // ─────────────────────────────────────────────────────────────────────────
//    @Override
//    @Transactional
//    public SessionResponse cancelSession(Long sessionId) {
//        log.info("Cancelling session id={}", sessionId);
//        Session session = findSessionOrThrow(sessionId);
//
//        if (session.getStatus() == SessionStatus.COMPLETED ||
//            session.getStatus() == SessionStatus.CANCELLED) {
//            throw new InvalidSessionStateException(
//                    "Cannot cancel a session that is already " + session.getStatus()
//            );
//        }
//
//        session.setStatus(SessionStatus.CANCELLED);
//        return sessionMapper.toResponse(sessionRepository.save(session));
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // GET /sessions/user/{userId}  →  Sessions where user is learner OR mentor
//    // ─────────────────────────────────────────────────────────────────────────
//    @Override
//    @Transactional(readOnly = true)
//    public List<SessionResponse> getSessionsByUserId(Long userId) {
//        log.info("Fetching sessions for userId={}", userId);
//        return sessionRepository.findByLearnerIdOrMentorId(userId, userId)
//                .stream()
//                .map(sessionMapper::toResponse)
//                .collect(Collectors.toList());
//    }
//
//    // ─────────────────────────────────────────────────────────────────────────
//    // Helpers
//    // ─────────────────────────────────────────────────────────────────────────
//
//    private Session findSessionOrThrow(Long sessionId) {
//        return sessionRepository.findById(sessionId)
//                .orElseThrow(() -> new SessionNotFoundException("Session not found with id: " + sessionId));
//    }
//
//    private void publishSessionBookedEvent(Session session) {
//        try {
//            SessionBookedEvent event = SessionBookedEvent.builder()
//                    .sessionId(session.getId())
//                    .mentorId(session.getMentorId())
//                    .learnerId(session.getLearnerId())
//                    .sessionTime(session.getSessionDate())
//                    .topic(session.getTopic())
//                    .eventType("SESSION_BOOKED")
//                    .build();
//
//            rabbitTemplate.convertAndSend(
//                    RabbitMQConfig.SESSION_EXCHANGE,
//                    RabbitMQConfig.SESSION_BOOKED_KEY,
//                    event
//            );
//
//            log.info("Published SESSION_BOOKED event for sessionId={}", session.getId());
//
//        } catch (Exception ex) {
//            log.error("Failed to publish event for sessionId={}", session.getId(), ex);
//
//            // THIS IS KEY → triggers rollback
//            throw new RuntimeException("Failed to publish session event");
//        }
//        
//        
//    }
}
