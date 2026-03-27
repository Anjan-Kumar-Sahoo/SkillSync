package com.skillsync.session.mapper;

import com.skillsync.session.dto.SessionResponse;
import com.skillsync.session.entity.Session;

/**
 * Pure mapping functions for Session entities.
 * Used by both SessionCommandService and SessionQueryService (CQRS decoupling).
 */
public final class SessionMapper {

    private SessionMapper() {}

    public static SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getId(), session.getMentorId(), session.getLearnerId(),
                session.getTopic(), session.getDescription(),
                session.getSessionDate(), session.getDurationMinutes(),
                session.getMeetingLink(), session.getStatus().name(),
                session.getCancelReason(), session.getCreatedAt()
        );
    }
}
