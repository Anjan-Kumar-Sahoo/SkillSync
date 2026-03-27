package com.skillsync.user.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MentorApprovedEvent {
    private Long mentorId;
    private Long userId;
    private String mentorName;
}
