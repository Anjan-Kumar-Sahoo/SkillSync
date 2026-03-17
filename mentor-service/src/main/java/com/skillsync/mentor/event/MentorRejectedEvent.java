package com.skillsync.mentor.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MentorRejectedEvent {
    private Long mentorId;
    private Long userId;
    private String reason;
}
