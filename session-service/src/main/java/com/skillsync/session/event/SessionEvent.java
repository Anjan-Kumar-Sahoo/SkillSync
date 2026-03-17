package com.skillsync.session.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @AllArgsConstructor @NoArgsConstructor
public class SessionEvent {
    private Long sessionId;
    private Long mentorId;
    private Long learnerId;
    private String topic;
    private String status;
    private String reason;
}
