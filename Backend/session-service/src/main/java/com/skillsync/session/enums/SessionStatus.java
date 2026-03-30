package com.skillsync.session.enums;

import java.util.Set;
import java.util.Map;

public enum SessionStatus {
    REQUESTED, ACCEPTED, REJECTED, COMPLETED, CANCELLED;

    private static final Map<SessionStatus, Set<SessionStatus>> ALLOWED_TRANSITIONS = Map.of(
            REQUESTED, Set.of(ACCEPTED, REJECTED, CANCELLED),
            ACCEPTED, Set.of(COMPLETED, CANCELLED),
            REJECTED, Set.of(),
            COMPLETED, Set.of(),
            CANCELLED, Set.of()
    );

    public boolean canTransitionTo(SessionStatus target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
